(ns org.altlaw.jobs.web.scrape
  (:require [org.altlaw.extract.scrape.handler :as handler]
            [org.altlaw.util.log :as log]
            [org.altlaw.util.files :as files]
            [org.altlaw.util.hadoop :as h])
  (:import (java.net URI)
           (org.apache.commons.codec.binary Base64)))

(h/setup-mapreduce)

(defn decode-response-body [download]
  (assoc download :response_body_bytes
         (Base64/decodeBase64
          (.getBytes (:response_body_base64 download) "UTF-8"))))

(defn mime-type [download]
  (files/guess-mime-type-by-content (:response_body_bytes download))  )

(defn handle-scraper-result [result]
  (cond
    (contains? result :exception)
    (do (log/warn (pr-str result))
        (h/counter "Scraper results" "exceptions")
        [:exception result])

    (contains? result :court)
    (do (h/counter "Scraper results" "found documents")
        [:document result])

    :else
    (do (h/counter "Scraper results" "UNRECOGNIZED")
        (log/warn "Unrecognized scraper result: " (pr-str result))
        [:unrecognized result])))

(defn my-map [line-number download]
  (let [download (decode-response-body download)
        type (mime-type download)]
    (h/counter "MIME Types" type)
    (if (= type "text/html")
      (map handle-scraper-result
           (handler/run-scrapers download))
      (do (log/debug "Ignoring MIME type " type)
          nil))))

(def mapper-map (partial h/standard-map my-map))

(defn tool-run [this args]
  (let [job (h/default-jobconf this)
        inpath (Path. (h/job-path :web :crawl))
        outpath (Path. (h/job-path :web :scrape))
        hdfs (FileSystem/get job)]
    (.delete hdfs outpath true)
    (FileInputFormat/setInputPaths job (str inpath "/c-*"))
    (FileOutputFormat/setOutputPath job outpath)
    (.setMapperClass job org.altlaw.jobs.web.scrape_mapper)
    ;; (.setReducerClass job org.altlaw.jobs.web.scrape_reducer)
    (.setNumReduceTasks job 0)
    (.setInputFormat job TextInputFormat)
    (.setOutputFormat job TextOutputFormat)
    (FileOutputFormat/setOutputCompressorClass job GzipCodec)
    (.setJobName job "web.scrape")
    (JobClient/runJob job))
  0)