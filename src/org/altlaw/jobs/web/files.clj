(ns org.altlaw.jobs.web.files
  (:require [org.altlaw.util.crawler :as crawler]
            [org.altlaw.db.download-log :as dl]
            [org.altlaw.util.log :as log]
            [org.altlaw.util.context :as context]
            [org.altlaw.util.files :as files]
            [org.altlaw.util.hadoop :as h]))

(h/setup-mapreduce)

(defn all-links [document]
  (mapcat val (:links document)))

(defn check-downloaded [url]
  (if (dl/downloaded? url)
    (do (h/counter "Link URLs" "Already downloaded")
        nil)
    (do (h/counter "Link URLs" "Requested")
        [{:request_uri url} nil])))

(defn my-map [record-type record]
  (when (= :document record-type)
    (filter identity
            (map check-downloaded (all-links record)))))

(defn my-reduce [request nils]
  (h/counter "Link URLs" "Requested after removing duplicates")
  [[request nil]])

(defn mapper-configure [this jobconf]
  (context/use-hadoop-jobconf jobconf))

(def mapper-map (partial h/standard-map my-map))

(def reducer-reduce (partial h/standard-reduce my-reduce))

(defn tool-run [this args]
  (let [job (h/default-jobconf this)
        inpath (Path. (h/job-path :web :scrape))
        outpath (Path. (h/job-path :web :request))
        hdfs (FileSystem/get job)]
    (.delete hdfs outpath true)
    (FileInputFormat/setInputPaths job (str inpath))
    (FileOutputFormat/setOutputPath job outpath)
    (FileOutputFormat/setCompressOutput job false)
    (.setMapperClass job org.altlaw.jobs.web.files_mapper)
    (.setReducerClass job org.altlaw.jobs.web.files_reducer)
    (.setInputFormat job KeyValueTextInputFormat)
    (.setOutputFormat job TextOutputFormat)
    (.setJobName job "web.files")
    (JobClient/runJob job))
  0)