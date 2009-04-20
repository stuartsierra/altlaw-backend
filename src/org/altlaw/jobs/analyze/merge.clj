(ns org.altlaw.jobs.analyze.merge
  (:refer clojure.set)
  (:require [org.altlaw.util.hadoop :as h]
            [org.altlaw.util.log :as log]
            [org.altlaw.db.privacy :as priv]
            [org.altlaw.util.merge-fields :as merge]))

(h/setup-mapreduce)


(defn my-reduce [docid documents]
  (if (priv/removed? docid)
    (do (h/counter "Removed documents")
        (log/info "Skipping removed document " docid)
        nil)
    [[docid (merge/merge-fields documents)]]))

;;; METHOD IMPLEMENTATIONS

(def reducer-reduce (partial h/standard-reduce my-reduce))

(defn tool-run [this args] []
  (let [job (h/default-jobconf this)
        outpath (Path. (h/job-path :analyze :merge))
        hdfs (FileSystem/get job)]
    (.setJobName job "merge")
    (FileInputFormat/setInputPaths job (str (h/job-path :pre :profed)
                                            "," (h/job-path :pre :ohm1)
                                            "," (h/job-path :analyze :cite_fields)))
    (FileOutputFormat/setOutputPath job outpath)
    (.delete hdfs outpath true)
    (.setMapperClass job IdentityMapper)
    (.setReducerClass job org.altlaw.jobs.analyze.merge_reducer)
    (JobClient/runJob job))
  0)

