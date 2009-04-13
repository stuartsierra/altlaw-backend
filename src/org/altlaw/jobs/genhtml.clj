(ns org.altlaw.jobs.genhtml
  (:require [org.altlaw.util.hadoop :as hadoop]
            [org.altlaw.util.log :as log]
            [org.altlaw.util.context :as context]
            [org.altlaw.www.case-pages :as cases]))

(hadoop/setup-mapreduce)

(defn mapper-configure [this jobconf]
  (context/use-property-function (fn [name] (.get jobconf name))))

(defn mapper-map [this wkey wvalue output reporter]
  (let [docid (.get wkey)
        doc (read-string (str wvalue))]
    (log/debug "Mapper got document " docid)
    (doseq [[filename content] (cases/all-files doc)]
      (.collect output (Text. filename) 
                (BytesWritable. (.getBytes content "UTF-8"))))))

(defn tool-run [this args]
  (let [job (hadoop/default-jobconf this)
        outpath (Path. (hadoop/job-path :genhtml))
        hdfs (FileSystem/get job)]
    (.setJobName job "genhtml")

    (FileInputFormat/setInputPaths job (hadoop/job-path :link :mergedocs))
    (FileOutputFormat/setOutputPath job outpath)
    (.delete hdfs outpath true)

    (.setMapperClass job (Class/forName
                          "org.altlaw.jobs.genhtml_mapper"))
    (.setNumReduceTasks job 0)

    (.setOutputKeyClass job Text)
    (.setOutputValueClass job BytesWritable)
    (JobClient/runJob job))
  0)
