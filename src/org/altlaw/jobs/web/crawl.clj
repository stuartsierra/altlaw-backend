(ns org.altlaw.jobs.web.crawl
  (:require [org.altlaw.extract.scrape.handler :as handler]
            [org.altlaw.util.log :as log]
            [org.altlaw.util.hadoop :as h]
            [org.altlaw.util.crawler :as crawler]
            [org.altlaw.util.context :as context]
            [org.altlaw.db.download-log :as dl]
            [clojure.contrib.duck-streams :as duck])
  (:import (java.net URI)))

(h/setup-mapreduce)

(defn my-map [line-number request]
  [[(.getHost (URI. (:request_uri request))), request]])

(defn handle-request [request]
  (let [response (crawler/handle-download-request request)
        code (:response_status_code response)]
    (h/counter "Response codes" code)
    (if (= 200 code)
      [response nil]
      (do (log/warn "HTTP response code " code " for " (:request_uri request))
          nil))))

(defn my-reduce [host requests]
  (filter identity
          (map handle-request requests)))

(def mapper-map (partial h/standard-map my-map))

(defn reducer-configure [this job]
  (context/use-hadoop-jobconf job))

(def reducer-reduce (partial h/standard-reduce my-reduce))

(defn reducer-close [this]
  (dl/save-download-log))

(defn tool-run [this args]
  (let [job (h/default-jobconf this)
        outpath (Path. (h/job-path :web :crawl))
        inpath (Path. (h/job-path :web :requests))
        hdfs (FileSystem/get job)]
    (.delete hdfs inpath true)
    (with-open [hdfs-stream (.create hdfs (Path. inpath "part-00000") true)]
      (binding [*out* (duck/writer (.getWrappedStream hdfs-stream))]
        (doseq [r (handler/all-requests)]
          (prn r))))
    (FileInputFormat/setInputPaths job (str inpath))
    (FileOutputFormat/setOutputPath job outpath)
    (FileOutputFormat/setOutputCompressorClass job GzipCodec)
    (.setInputFormat job TextInputFormat)
    (.setOutputFormat job TextOutputFormat)
    (.delete hdfs outpath true)
    (.setMapperClass job org.altlaw.jobs.web.crawl_mapper)
    (.setReducerClass job org.altlaw.jobs.web.crawl_reducer)
    (.setNumReduceTasks job 1)
    (.setJobName job "web.crawl")
    (JobClient/runJob job))
  0)