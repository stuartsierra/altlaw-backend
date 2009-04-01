(ns org.altlaw.jobs.procfiles.profed
    (:use clojure.contrib.trace org.altlaw.util.hadoop)
    (:require org.altlaw.jobs.procfiles.profed-map)
    (:gen-class
     :extends org.apache.hadoop.conf.Configured
     :implements [org.apache.hadoop.util.Tool]))

(import-hadoop)

(defn -run [this args] []
  (let [job (default-jobconf this)
        outpath (Path. (job-path :procfiles :profed))
        hdfs (FileSystem/get job)]
    (.setJobName job "procfiles.profed")
    (FileInputFormat/setInputPaths job (str (job-path :seqfiles :profed) "/*.seq"))
    (FileOutputFormat/setOutputPath job outpath)
    (.delete hdfs outpath true)
    (.setMapperClass job org.altlaw.jobs.procfiles.profed_map)
    (.setNumReduceTasks job 0)
    (DistributedCache/addCacheFile (URI. (docid-path :seqfiles :profed)) job)
    (JobClient/runJob job))
  0)

(defn -main [& args]
  (System/exit
   (ToolRunner/run (Configuration.)
                   (.newInstance (Class/forName "org.altlaw.jobs.procfiles.profed"))
                   (into-array String args))))

