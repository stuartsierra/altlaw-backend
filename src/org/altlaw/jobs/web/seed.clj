(ns org.altlaw.jobs.web.seed
  (:require [org.altlaw.util.hadoop :as h]
            [org.altlaw.extract.scrape.handler :as handler]
            [clojure.contrib.duck-streams :as duck]))

(h/setup-mapreduce)

(defn tool-run [this args]
  (let [job (h/default-jobconf this)
        outpath (Path. (h/job-path :web :request))
        hdfs (FileSystem/get job)]
    (.delete hdfs outpath true)
    (with-open [hdfs-stream (.create hdfs (Path. outpath "part-99999") true)]
      (binding [*out* (duck/writer (.getWrappedStream hdfs-stream))]
        (doseq [r (handler/all-requests)]
          (prn r))))
    0))
