(ns org.altlaw.jobs.pre.altcrawl
  (:require [org.altlaw.util.hadoop :as h]
            [org.altlaw.util.files :as files]
            [org.altlaw.util.tsv :as tsv]
            [org.altlaw.util.jruby :as ruby]
            [org.altlaw.util.context :as context]
            [org.altlaw.util.log :as log]
            [org.altlaw.util.merge-fields :as merge]
            [org.altlaw.db.docids :as ids]
            [org.altlaw.extract.pdf :as pdf]
            [clojure.contrib.walk :as walk]
            [clojure.contrib.singleton :as sing]
            [clojure.set :as set])
  (:use clojure.contrib.json.read
        org.altlaw.util.log)
  (:import (org.altlaw.extract PROHTMLToText)
           (org.apache.commons.codec.binary Base64)
           (javax.script ScriptContext ScriptEngine ScriptEngineManager)
           (java.io StringReader)))

(h/setup-mapreduce)

(def scraper-handler
     (sing/per-thread-singleton
      (fn []
        (ruby/eval-jruby "require 'org/altlaw/extract/scrape/scraper_handler'")
        (ruby/eval-jruby "ScraperHandler.new"))))

(defn- decode-response-body [download]
  (assoc download :response_body_bytes
         (Base64/decodeBase64
          (.getBytes (:response_body_base64 download) "UTF-8"))))


;; DIAGRAM OF CONTROL FLOW
;;
;; status-code-dispatch
;;  |                |
;;  |               warn
;;  |
;; mime-type-dispatch----------------------
;;  |                   |                 |
;;  |                   |                 |
;; handle-html         pdf-docid-dispatch        warn
;;  |       |           |      |              
;;  |      warn         |     warn
;;  |                   |                     
;; scraper-dispatch    convert-pdf            
;;  |
;; (for each case found)
;;  |
;;  |
;; handle-scraped
;;  |




;; Step 3 (for HTML): Run scrapers

(defn get-primary-url [record]
  (when-let [links (get record "links")]
    (get links "application/pdf")))

(defn get-all-urls [record]
  (when-let [links (get record "links")]
    (vals links)))

(defn rubyhash-to-map [hash]
  (reduce (fn [m [k v]]
            (assoc m (keyword k)
                   (if (instance? java.util.Map v)
                     (into {} v)  ;; don't keywordize MIME types
                     (str v))))
          {} hash))

(defn handle-scraper-record [record]
  (let [data (into {} record)]
    (if (contains? data "exception")
      (do (h/counter "Scraper errors")
          [:scraper-error (rubyhash-to-map data)])
      (if-let [url (get-primary-url data)]
        (if-let [docid (ids/get-docid "altcrawl" url)]
          [docid (rubyhash-to-map data)]
          [:docid-request (ids/make-docid-request "altcrawl" (get-all-urls data))])
        (do (h/counter "No primary URL")
            (log/warn "No primary URL among " (pr-str (get-all-urls data)))
            nil)))))

(defn run-scrapers [download]
  (filter identity
          (map handle-scraper-record
               (ruby/eval-jruby "$handler.parse(Download.from_map($dmap))"
                                {:dmap (walk/stringify-keys download)
                                 :handler (scraper-handler)}))))


;;; Step 4 (for PDFs): convert to HTML

(defn convert-pdf [docid download]
  (debug "converting PDF to HTML for " (:request_uri download))
  (let [html (pdf/pdf-to-html (str docid) (:response_body_bytes download))
        text (PROHTMLToText/filter html)]
    [[docid {:html html, :text text}]]))

;;; Step 3 (for PDFs): dispatch on presence of Docid

(defn pdf-docid-dispatch [download]
  (if-let [docid (ids/get-docid "altcrawl" (:request_uri download))]
    (do (debug "pdf-docid-dispatch got docid " docid " for " (:request_uri download))
        (h/counter "PDF files" "with docid")
        (convert-pdf docid download))
    (do (warn "pdf-docid-dispatch found no docid for " (:request_uri download))
        (h/counter "PDF files" "no docid")
        nil)))


;;; Step 2: dispatch on MIME type of the download

(defmulti mime-type-dispatch
  (fn [download]
    (files/guess-mime-type-by-content (:response_body_bytes download))))

(defmethod mime-type-dispatch "application/pdf" [download]
  (h/counter "MIME types" "PDF")
  (debug "mime-type-dispatch got PDF from " (:request_uri download))
  (pdf-docid-dispatch download))

(defmethod mime-type-dispatch "text/html" [download]
  (h/counter "MIME types" "HTML")
  (debug "mime-type-dispatch got HTML from " (:request_uri download))
  (run-scrapers download))

(defmethod mime-type-dispatch :default [download]
  (h/counter "MIME types" "unknown")
  (warn "mime-type-dispatch found no handler for " (:request_uri download)))


;;; Step 1: dispatch on Status code of the download

(defn status-code-dispatch [download]
  (h/counter "Status codes" (str (:response_status_code download)))
  (if (= 200 (:response_status_code download))
    (mime-type-dispatch download)
    (warn "status-code-dispatch saw "
          (:response_status_code download)
          " from " (:request_uri download))))


(defn my-map [key download]
  (status-code-dispatch (decode-response-body download)))



;;; REDUCER IMPLEMENTATION

(defn merge-docs [docid documents]
  (let [doc (merge/merge-fields documents)]
    (if (and (:html doc) (:text doc))
      [[docid doc]]
      (do (h/counter "No html/text")
          (log/warn "No html/text; skipping " docid)))))

(defn assign-docids [requests]
  (doseq [r requests]
    (ids/handle-docid-request r))
  (ids/save-docids "altcrawl")
  (ids/save-next-docid)
  nil)

(defn my-reduce [key documents]
  (cond
    (= key :docid-request) (assign-docids documents)
    (= key :exception) nil
    (integer? key) (merge-docs key documents)))



;;; MAPPER METHODS

(defn mapper-configure [this job]
  (context/use-hadoop-jobconf job))

(def mapper-map (partial h/json-map my-map))



;;; REDUCER METHODS

(defn reducer-configure [this job]
  (context/use-hadoop-jobconf job))

(def reducer-reduce (partial h/standard-reduce my-reduce))




;;; TOOL METHODS

(defn tool-run [this args]
  (let [job (h/default-jobconf this)
        outpath (Path. (h/job-path :pre :altcrawl))
        hdfs (FileSystem/get job)]
    (.setJobName job "pre.altcrawl")
    (FileInputFormat/setInputPaths job (str (h/job-path :seqfiles :altcrawl) "/*.jsr"))
    (FileOutputFormat/setOutputPath job outpath)
    (.setInputFormat job TextInputFormat)
    (.delete hdfs outpath true)
    (.setMapperClass job (Class/forName "org.altlaw.jobs.pre.altcrawl_mapper"))
    (.setReducerClass job (Class/forName "org.altlaw.jobs.pre.altcrawl_reducer"))
    (.setMapOutputKeyClass job Text)
    (DistributedCache/addCacheFile (URI. (h/docid-path :seqfiles :ohm1)) job)
    (JobClient/runJob job))
  0)
