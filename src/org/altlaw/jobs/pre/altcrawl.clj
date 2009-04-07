(ns org.altlaw.jobs.pre.altcrawl
  (:require [org.altlaw.util.hadoop :as hadoop]
            [org.altlaw.util.files :as files]
            [org.altlaw.util.tsv :as tsv]
            [clojure.contrib.walk :as walk])
  (:use clojure.contrib.json.read
        org.altlaw.util.log)
  (:import (org.apache.commons.codec.binary Base64)))

(hadoop/setup-mapreduce)

(declare *reporter*)

(defn counter [group name]
  (.incrCounter *reporter* group name 1))

(def *docid-map* (ref {}))

(defn get-docid [uri]
  (get @*docid-map* uri))

(defn- decode-response-body [download]
  (assoc download :response_body_bytes
         (Base64/decodeBase64
          (.getBytes (:response_body_base64 download) "UTF-8"))))

;;; Step 2: dispatch on MIME type of the download

(defmulti mime-type-dispatch
  (fn [download]
    (files/guess-mime-type-by-content (:response_body_bytes download))))

(defmethod mime-type-dispatch "application/pdf" [download]
  (counter "MIME types" "PDF")
  (debug "mime-type-dispatch got PDF from " (:request_uri download)))

(defmethod mime-type-dispatch "text/html" [download]
  (counter "MIME types" "HTML")
  (debug "mime-type-dispatch got HTML from " (:request_uri download)))

(defmethod mime-type-dispatch :default [download]
  (counter "MIME types" "unknown")
  (warn "mime-type-dispatch found no handler for " (:request_uri download)))


;;; Step 1: dispatch on Status code of the download

(defn status-code-dispatch [download]
  (counter "Status codes" (str (:response_status_code download)))
  (if (= 200 (:response_status_code download))
    (mime-type-dispatch download)
    (warn "status-code-dispatch saw "
          (:response_status_code download)
          " from " (:request_uri download))))


(defn my-map [download]
  (status-code-dispatch (decode-response-body download)))



;;; MAPPER METHODS

(defn mapper-configure [this job]
  (let [cached (DistributedCache/getLocalCacheFiles job)
        path (first (filter #(= (.getName %) "docids.tsv.gz") cached))]
    (when (nil? path) (throw (RuntimeException. "No docid.tsv.gz in DistributedCache.")))
    (info "Loading docid map from " path)
    (dosync (ref-set *docid-map* (tsv/load-tsv-map (str path))))))

(defn mapper-map [this wkey wvalue output reporter]
    (let [input (walk/keywordize-keys (read-json-string (str wvalue)))]
      (debug "Called map with file position " wkey
             " and value " (logstr input))
      (binding [*reporter* reporter]
        (doseq [[key value] (my-map input)]
          (.collect output (Text. key) (Text. value))))))



;;; REDUCER METHODS

(defn reducer-reduce [this wkey values-iter output reporter]
  (debug "Called reduce with key " wkey " and "
         (count (iterator-seq values-iter)) " values."))



;;; TOOL METHODS

(defn tool-run [this args]
  (let [job (hadoop/default-jobconf this)
        outpath (Path. (hadoop/job-path :pre :altcrawl))
        hdfs (FileSystem/get job)]
    (.setJobName job "pre.altcrawl")
    (FileInputFormat/setInputPaths job (str (hadoop/job-path :seqfiles :altcrawl) "/*.jsr"))
    (FileOutputFormat/setOutputPath job outpath)
    (.setInputFormat job TextInputFormat)
    (.delete hdfs outpath true)
    (.setMapperClass job (Class/forName "org.altlaw.jobs.pre.altcrawl_mapper"))
    (.setReducerClass job (Class/forName "org.altlaw.jobs.pre.altcrawl_reducer"))
    (.setMapOutputKeyClass job Text)
    (DistributedCache/addCacheFile (URI. (hadoop/docid-path :seqfiles :ohm1)) job)
    (JobClient/runJob job))
  0)
