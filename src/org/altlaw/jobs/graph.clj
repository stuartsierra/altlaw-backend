(ns org.altlaw.jobs.graph
    (:use clojure.contrib.trace org.altlaw.util.hadoop)
    (:require org.altlaw.jobs.graph-map)
    (:gen-class
     :extends org.apache.hadoop.conf.Configured
     :implements [org.apache.hadoop.util.Tool]))

(import-hadoop)

(defn -run [this args] []
  (let [job (default-jobconf this)
        outpath (Path. (job-path :graph))
        hdfs (FileSystem/get job)]
    (.setJobName job "graph")
    (FileInputFormat/setInputPaths job (str (job-path :procfiles :profed)
                                            "," (job-path :procfiles :ohm1)))
    (FileOutputFormat/setOutputPath job outpath)
    (.delete hdfs outpath true)
    (.setMapperClass job org.altlaw.jobs.graph_map)
    (.setReducerClass job org.altlaw.jobs.graph_map)
    (.setMapOutputKeyClass job Text)
    (.setMapOutputValueClass job Text)
    (.setOutputKeyClass job IntWritable)
    (.setOutputValueClass job IntWritable)
    (JobClient/runJob job))
  0)

(defn -main [& args]
  (System/exit
   (ToolRunner/run (Configuration.)
                   (.newInstance (Class/forName "org.altlaw.jobs.graph"))
                   (into-array String args))))

