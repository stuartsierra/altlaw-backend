(ns org.altlaw.jobs.web.merge
  (:require [org.altlaw.util.log :as log]
            [org.altlaw.util.hadoop :as h]
            [org.altlaw.db.docids :as ids]
            [org.altlaw.util.context :as context]
            [org.altlaw.extract.pdf :as pdf]
            [org.altlaw.util.merge-fields :as merge])
  (:import (org.altlaw.extract Anonymizer PROHTMLToText)
           (org.apache.commons.codec.binary Base64)))

(h/setup-mapreduce)

;;; MAPPER IMPLEMENTATION

(defn all-links [document]
  (mapcat val (:links document)))

(defn decode-response-body [download]
  (assoc download :response_body_bytes
         (Base64/decodeBase64
          (.getBytes (:response_body_base64 download) "UTF-8"))))

(defn map-document [document]
  (let [links (all-links document)]
    (if-let [docid (some #(ids/get-docid "altcrawl" %) (all-links document))]
      [[docid (assoc document :docid docid)]]
      (do (log/warn "Missing docid for " links)
          (h/counter "Missing docid")
          nil))))

(defn map-download [download]
  (if-let [docid (ids/get-docid "altcrawl" (:request_uri download))]
    (let [download (decode-response-body download)
          html (Anonymizer/filter (pdf/pdf-to-html (str docid) (:response_body_bytes download)))
          text (PROHTMLToText/filter html)]
      [[docid {:docid docid :html html :text text}]])
    (do (log/warn "Missing docid for " (:request_uri download))
        (h/counter "Missing docid")
        nil)))

(defn my-map [key value]
  (cond
    (= key :document) (map-document value)
    (map? key) (map-download key)
    :else (do (log/warn "Unrecognized record type " (log/logstr key) " => " (log/logstr value))
              (h/counter "Unrecognized record types")
              nil)))


;;; REDUCER IMPLEMENTATION

(defn merge-docs [docid documents]
  (let [doc (merge/merge-fields documents)]
    (if (and (:html doc) (:text doc))
      [[docid doc]]
      (do (h/counter "No html/text")
          (log/warn "No html/text; skipping " docid)
          nil))))

(defn my-reduce [docid documents]
  (merge-docs docid documents))


;;; MAPPER METHODS

(defn mapper-configure [this job]
  (context/use-hadoop-jobconf job))

(def original-read-string read-string)

(defn my-read-string [s]
  (if (empty? s) nil
      (original-read-string s)))

(defn mapper-map [this key value output reporter]
  (binding [read-string my-read-string]
    (h/standard-map my-map this key value output reporter)))


;;; REDUCER METHODS

(defn reducer-configure [this job]
  (context/use-hadoop-jobconf job))

(def reducer-reduce (partial h/standard-reduce my-reduce))


;;; TOOL METHODS

(defn tool-run [this args]
  (let [job (h/default-jobconf this)
        outpath (Path. (h/job-path :web :merge))
        hdfs (FileSystem/get job)]
    (.delete hdfs outpath true)
    (FileInputFormat/setInputPaths job (str (h/job-path :web :scrape) ","
                                            (h/job-path :web :crawl) "/c-*"))
    (FileOutputFormat/setOutputPath job outpath)
    (.setInputFormat job KeyValueTextInputFormat)
    (.setMapperClass job org.altlaw.jobs.web.merge_mapper)
    (.setReducerClass job org.altlaw.jobs.web.merge_reducer)
    (.setJobName job "web.merge")
    (JobClient/runJob job))
  0)
