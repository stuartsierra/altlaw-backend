(ns org.altlaw.jobs.analyze.cite_fields
  (:refer clojure.set)
  (:require [org.altlaw.util.hadoop :as h]
            [org.altlaw.util.log :as log]))

(h/setup-mapreduce)

(defn my-map [source-docid target-docid]
  [[source-docid {:outcites #{target-docid}}]
   [target-docid {:incites #{source-docid}}]])

(defn my-reduce [docid documents]
  [[docid (apply merge-with union documents)]])



;;; METHOD IMPLEMENTATIONS

(def mapper-map (partial h/standard-map my-map))

(def reducer-reduce (partial h/standard-reduce my-reduce))

(defn tool-run [this args] []
  (let [job (h/default-jobconf this)
        outpath (Path. (h/job-path :cite_fields))
        hdfs (FileSystem/get job)]
    (.setJobName job "cite_fields")
    (FileInputFormat/setInputPaths job (str (h/job-path :graph)))
    (FileOutputFormat/setOutputPath job outpath)
    (.delete hdfs outpath true)
    (.setMapperClass job org.altlaw.jobs.analyze.cite_fields_mapper)
    (.setReducerClass job org.altlaw.jobs.analyze.cite_fields_reducer)
    (JobClient/runJob job))
  0)
