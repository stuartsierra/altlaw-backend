(ns org.altlaw.jobs.procfiles.ohm1
    (:use clojure.contrib.trace org.altlaw.util.hadoop)
    (:require org.altlaw.jobs.procfiles.ohm1-map)
    (:gen-class
     :extends org.apache.hadoop.conf.Configured
     :implements [org.apache.hadoop.util.Tool]))

(import-hadoop)

(defn -run [this args] []
  (let [job (default-jobconf this)
        outpath (Path. (job-path :procfiles :ohm1))
        hdfs (FileSystem/get job)]
    (.setJobName job "procfiles.ohm1")
    (FileInputFormat/setInputPaths job (str (job-path :seqfiles :ohm1) "/*.seq"))
    (FileOutputFormat/setOutputPath job outpath)
    (.delete hdfs outpath true)
    (.setMapperClass job org.altlaw.jobs.procfiles.ohm1_map)
    (.setReducerClass job org.altlaw.jobs.procfiles.ohm1_map)
    ; (.setNumReduceTasks job 0)
    (DistributedCache/addCacheFile (URI. (docid-path :seqfiles :ohm1)) job)
    (JobClient/runJob job))
  0)

(defn -main [& args]
  (System/exit
   (ToolRunner/run (Configuration.)
                   (.newInstance (Class/forName "org.altlaw.jobs.procfiles.ohm1"))
                   (into-array String args))))

