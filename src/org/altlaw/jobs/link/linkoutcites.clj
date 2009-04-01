(ns org.altlaw.jobs.link.linkoutcites
    (:use clojure.contrib.trace org.altlaw.util.hadoop)
    (:require org.altlaw.jobs.link.linkoutcites-mapred)
    (:gen-class
     :extends org.apache.hadoop.conf.Configured
     :implements [org.apache.hadoop.util.Tool]))

(import-hadoop)

(defn -run [this args] []
  (let [job (default-jobconf this)
        outpath (Path. (job-path :link :linkoutcites))
        hdfs (FileSystem/get job)]
    (.setJobName job "linkoutcites")
    (FileInputFormat/setInputPaths job (str (job-path :graph)
                                            "," (job-path :procfiles :ohm1)
                                            "," (job-path :procfiles :profed)))
    (FileOutputFormat/setOutputPath job outpath)
    (.delete hdfs outpath true)
    (.setMapperClass job org.altlaw.jobs.link.linkoutcites_mapred)
    (.setReducerClass job org.altlaw.jobs.link.linkoutcites_mapred)
    (.setOutputKeyClass job IntWritable)
    (.setOutputValueClass job Text)
    (JobClient/runJob job))
  0)

(defn -main [& args]
  (System/exit
   (ToolRunner/run (Configuration.)
                   (.newInstance (Class/forName "org.altlaw.jobs.link.linkoutcites"))
                   (into-array String args))))

