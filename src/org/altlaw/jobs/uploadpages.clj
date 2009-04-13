(ns org.altlaw.jobs.uploadpages
  (:require [org.altlaw.util.hadoop :as hadoop]
            [org.altlaw.util.log :as log]
            [org.altlaw.util.context :as context]
            [org.altlaw.db.content :as content])
  (:import (java.util Arrays)))

(hadoop/setup-mapreduce)

(use 'clojure.contrib.repl-utils)

(defn mapper-configure [this jobconf]
  (context/use-property-function (fn [name] (.get jobconf name))))

(def *empty-bytes* (BytesWritable.))

(defn mapper-map [this wkey wvalue output reporter]
  (log/debug "Mapper got file " wkey)
  (content/put-page-bytes (str wkey) 
                          (Arrays/copyOf (.get wvalue) (.getSize wvalue))
                          "text/html")
  (.incrCounter reporter "org.altlaw.jobs.uploadpages" "Uploaded objects" 1)
  (.collect output wkey *empty-bytes*))

(defn tool-run [this args]
  (let [job (hadoop/default-jobconf this)
        outpath (Path. (hadoop/job-path :uploadpages))
        hdfs (FileSystem/get job)]
    (.setJobName job "uploadpages")

    (FileInputFormat/setInputPaths job (hadoop/job-path :genhtml))
    (FileOutputFormat/setOutputPath job outpath)
    (.delete hdfs outpath true)

    (.setMapperClass job (Class/forName
                          "org.altlaw.jobs.uploadpages_mapper"))
    (.setNumReduceTasks job 0)

    (.setOutputKeyClass job Text)
    (.setOutputValueClass job BytesWritable)
    (JobClient/runJob job))
  0)
