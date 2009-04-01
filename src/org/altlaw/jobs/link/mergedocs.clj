(ns org.altlaw.jobs.link.mergedocs
    (:use clojure.contrib.trace org.altlaw.util.hadoop)
    (:require org.altlaw.jobs.link.mergedocs-mapred)
    (:gen-class
     :extends org.apache.hadoop.conf.Configured
     :implements [org.apache.hadoop.util.Tool]))

(import-hadoop)

(defn -run [this args] []
  (let [#^JobConf job (default-jobconf this)
        outpath (Path. (job-path :link :mergedocs))
        hdfs (FileSystem/get job)]
    (.setJobName job "mergedocs")
    (FileInputFormat/setInputPaths job (str (job-path :link :linkincites)
                                            "," (job-path :link :linkoutcites)
                                            "," (job-path :procfiles :ohm1)
                                            "," (job-path :procfiles :profed)))
    (FileOutputFormat/setOutputPath job outpath)
    (.delete hdfs outpath true)
    (.setMapperClass job IdentityMapper)
    (.setReducerClass job org.altlaw.jobs.link.mergedocs_mapred)
    (.setOutputKeyClass job IntWritable)
    (.setOutputValueClass job Text)
    
    (println "Internal base URI is " (.get job "org.altlaw.internal.base"))

    (JobClient/runJob job))
  0)

(defn -main [& args]
  (System/exit
   (ToolRunner/run (Configuration.)
                   (.newInstance (Class/forName "org.altlaw.jobs.link.mergedocs"))
                   (into-array String args))))

