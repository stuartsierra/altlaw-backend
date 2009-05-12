(ns org.altlaw.jobs.post.makecontent
  (:require [org.altlaw.util.hadoop :as h]
            [org.altlaw.util.log :as log]
            [org.altlaw.util.context :as context]
            [org.altlaw.db.content :as content])
  (:import (org.apache.commons.codec.digest DigestUtils)))

(h/setup-mapreduce)

(defn my-map [docid doc]
  (if-let [html (:html doc)]
    (let [filename (str "sha1/" (DigestUtils/shaHex html))]
      [filename html])
    (do (h/counter "No HTML")
        (log/warn "No HTML for docid " docid)
        nil)))

(defn mapper-map [this wkey wvalue output reporter]
  (binding [h/*reporter* reporter]
   (let [[key value] (my-map (read-string (str wkey))
                             (read-string (str wvalue)))]
     (when (and key value)
       (.collect output (Text. key) (Text. value))))))

(defn tool-run [this args]
  (let [job (h/default-jobconf this)
        outpath (Path. (h/job-path :post :content))]
    (.setJobName job "post.content")
    (FileInputFormat/setInputPaths job (h/job-path :analyze :merge))
    (FileOutputFormat/setOutputPath job outpath)
    (.delete (FileSystem/get job) outpath true)
    (.setMapperClass job org.altlaw.jobs.post.makecontent_mapper)
    (.setReducerClass job IdentityReducer)
    (JobClient/runJob job))
  0)
