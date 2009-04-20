(ns org.altlaw.jobs.savecontent
  (:require [org.altlaw.util.hadoop :as h]
            [org.altlaw.util.log :as log]
            [org.altlaw.util.context :as context]
            [org.altlaw.db.content :as content]))

(h/setup-mapreduce)

(defn my-map [docid doc]
  (if (:html doc)
    (do (h/counter "Put content")
        (let [hash (content/put-content-string
                    (:html doc) {"Content-Type" "text/html"})]
          [[docid {:html_content_sha1 hash}]]))
    (do (h/counter "No HTML")
        (log/warn "No HTML for docid " docid)
        nil)))

(def mapper-map (partial h/standard-map my-map))

(defn tool-run [this args]
  (let [job (h/default-jobconf this)
        outpath (Path. (h/job-path :post :savecontent))]
    (.setJobName job "savecontent")
    (FileInputFormat/setInputPaths job (h/job-path :analyze :merge))
    (FileOutputFormat/setOutputPath job outpath)
    (.delete (FileSystem/get job) outpath true)
    (.setMapperClass job org.altlaw.jobs.post.savecontent_mapper)
    (.setNumReduceTasks job 0)
    (JobClient/runJob job))
  0)
