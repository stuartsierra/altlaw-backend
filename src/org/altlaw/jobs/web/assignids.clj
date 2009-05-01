(ns org.altlaw.jobs.web.assignids
  (:require [org.altlaw.util.log :as log]
            [org.altlaw.util.hadoop :as h]
            [org.altlaw.db.docids :as ids]
            [org.altlaw.util.context :as context]))

(h/setup-mapreduce)

(defn all-links [document]
  (mapcat val (:links document)))

(defn my-map [sequence-type document]
  (let [links (all-links document)]
    (when (some #(nil? (ids/get-docid "altcrawl" %)) links)
      (h/counter "Docid requests")
      [[:docid-request (ids/make-docid-request "altcrawl" links)]])))

(defn handle-request [request]
  (ids/handle-docid-request request)
  (h/counter "Docid assignments")
  [:docid-assigned nil])

(defn my-reduce [key requests]
  (map handle-request requests))


;;; MAPPER METHODS

(defn mapper-configure [this job]
  (context/use-hadoop-jobconf job))

(def mapper-map (partial h/standard-map my-map))


;;; REDUCER METHODS

(defn reducer-configure [this job]
  (context/use-hadoop-jobconf job))

(def reducer-reduce (partial h/standard-reduce my-reduce))

(defn reducer-close [this]
  (ids/save-docids "altcrawl"))


(defn tool-run [this args]
  (let [job (h/default-jobconf this)
        inpath (Path. (h/job-path :web :scrape))
        outpath (Path. (h/job-path :web :assignids))
        hdfs (FileSystem/get job)]
    (.delete hdfs outpath true)
    (FileInputFormat/setInputPaths job (str inpath))
    (FileOutputFormat/setOutputPath job outpath)
    (.setMapperClass job org.altlaw.jobs.web.assignids_mapper)
    (.setReducerClass job org.altlaw.jobs.web.assignids_reducer)
    (.setNumReduceTasks job 1)
    (.setJobName job "web.assignids")
    (JobClient/runJob job))
  0)