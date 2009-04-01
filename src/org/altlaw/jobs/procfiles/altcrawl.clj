(ns org.altlaw.jobs.procfiles.altcrawl
  (:require [org.altlaw.util.hadoop :as hadoop]
            [clojure.contrib.walk :as walk])
  (:use clojure.contrib.json.read
        org.altlaw.util.log)
  (:import (org.apache.commons.codec.binary Base64)
           (java.util Arrays)))

(hadoop/setup-mapreduce)

(declare *reporter*)

(defn- get-bytes [download]
  (Base64/decodeBase64
   (.getBytes (:response_body_base64 download)
              "UTF-8")))

(defn- guess-mime-type
  "Attempts to guess the MIME type of the data in a byte array.
  Recognizes PDF and HTML.  Returns nil if it can't recognize the
  type."
  [bytes]
  (let [magic (String. (Arrays/copyOf bytes 4) "UTF-8")]
    (cond (= "%PDF" magic) "application/pdf"
          (.contains magic "<") "text/html"
          :else nil)))

(defmulti handle-download
  (fn [download] (guess-mime-type (get-bytes download))))

(defmethod handle-download :default [download]
  (.warn *log* (str "No handler for MIME type of "
                    (:request_uri download))))

(defmethod handle-download "application/pdf" [download]
  (.debug *log* (str "handle-download got PDF from "
                    (:request_uri download))))

(defmethod handle-download "text/html" [download]
  (.debug *log* (str "handle-download got HTML from "
                    (:request_uri download))))

(defmulti my-map
  (fn [data]
    (cond (contains? data :request_uri) :download
          (contains? data :doctype) :metadata)))

(defmethod my-map :download [data]
  (let [uri (:request_uri data)
        status (:response_status_code data)]
    (.debug *log* (str "my-map got download from " uri))
    (if (= 200 status)
      (handle-download data)
      (.warn *log* (str "my-map saw HTTP error " status
                        " from " uri)))))

(defmethod my-map :metadata [data]
  (.debug *log* (str "my-map got metadata for "
                     (:name data))))

(defn mapper-map [this wkey wvalue output reporter]
    (let [input (walk/keywordize-keys (read-json-string (str wvalue)))]
      (.debug *log* (str "Called map with file position " wkey
                         " and value " (logstr input)))
      (binding [*reporter* reporter]
        (doseq [[key value] (my-map input)]
          (.collect output (Text. key) (Text. value))))))

(defn reducer-reduce [this wkey wvalues output reporter]
  (.debug *log* (str "Called reduce with key " wkey)))

(defn tool-run [this args]
  (let [job (hadoop/default-jobconf this)
        outpath (Path. (hadoop/job-path :procfiles :altcrawl))
        hdfs (FileSystem/get job)]
    (.setJobName job "procfiles.altcrawl")
    (FileInputFormat/setInputPaths job (str (hadoop/job-path :seqfiles :altcrawl) "/*.jsr"))
    (FileOutputFormat/setOutputPath job outpath)
    (.setInputFormat job TextInputFormat)
    (.delete hdfs outpath true)
    (.setMapperClass job (Class/forName "org.altlaw.jobs.procfiles.altcrawl_mapper"))
    (.setReducerClass job (Class/forName "org.altlaw.jobs.procfiles.altcrawl_reducer"))
    (.setMapOutputKeyClass job Text)
    (JobClient/runJob job))
  0)
