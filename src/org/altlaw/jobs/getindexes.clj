(ns org.altlaw.jobs.getindexes
  (:require [org.altlaw.util.hadoop :as hadoop]
            [org.altlaw.util.zip :as zip]
            [clojure.contrib.duck-streams :as duck])
  (:gen-class
   :extends org.apache.hadoop.conf.Configured
   :implements [org.apache.hadoop.util.Tool]
   :prefix "tool-"
   :main true))

(hadoop/import-hadoop)

(defn tool-run [this args]
  (let [conf (.getConf this)
        fs (FileSystem/get conf)
        lines (duck/read-lines (.open fs (Path. "/v4/distindex/part-00000")))
        files (map #(first (.split % "\t")) lines)
        zipfiles (map #(str "/v4/distindex/" %) files)
        unzipdirs (map #(str "/mnt/mergeindex/" (.replace % ".zip" "")) files)]
    (println "Unpacking indexed ZIP files...")
    (doseq [[zipfile unzipdir] (map vector zipfiles unzipdirs)]
      (println zipfile)
      (with-open [stream (.open fs (Path. zipfile))]
        (zip/unzip-stream stream (duck/file unzipdir)))))
  0)

(defn tool-main [& args]
  (System/exit
   (org.apache.hadoop.util.ToolRunner/run 
    (Configuration.)
    (.newInstance (Class/forName "org.altlaw.jobs.getindexes"))
    (into-array String args))))
