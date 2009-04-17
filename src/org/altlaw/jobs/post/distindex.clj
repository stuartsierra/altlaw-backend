(ns org.altlaw.jobs.post.distindex
   (:require [org.altlaw.util.solr :as solr]
             [org.altlaw.util.zip :as zip]
             [org.altlaw.util.log :as log]
             [org.altlaw.util.hadoop :as h])
  (:import (org.apache.commons.io FileUtils)
           (java.io File)))

(h/setup-mapreduce)


;;; MAPPER CONFIGURATION

(def *config* (ref {}))

(defn setup-mapper [taskid index-root]
  (let [work-solr-home (File. index-root taskid)
        zip-file-name (str taskid "-index.zip")]

    (log/info "Task ID is " taskid)
    (log/info "Working Solr home is " work-solr-home)
    (log/info "Index ZIP file name is " zip-file-name)

    ;; Start the embedded Solr server:
    (let [[server core] (solr/start-embedded-solr work-solr-home)]
      ;; Set the configuration state:
      (dosync (alter *config* merge
                     {:taskid taskid
                      :work-solr-home work-solr-home
                      :zip-file-name zip-file-name
                      :server server
                      :core core})))))


;;; MAPPER STARTUP

(defn mapper-init
  "Constructor, passes all args to superclass constructor and
  initializes state to a ref of a map."
  [& args] [(vec args) (ref {})])


(defn mapper-configure
  "configure method of the Mapper.  Gets the necessary details out of
  the JobConf, lets setup-mapper do the rest.  Sets state of the Mapper
  to the return value of setup-mapper."
  [this jobconf]
  (let [taskid (.get jobconf "mapred.task.id")
        index-root (.get jobconf "org.altlaw.tmp.index.root"
                         "/mnt/distindex")]
    (setup-mapper taskid index-root)
    (dosync (alter *config* assoc :jobconf jobconf))))



;;; MAPPER RUN

(defn prepare-solr-document [data]
  (select-keys data [:docid :doctype :name :citations
                     :court :text :size :date]))

(defn index-document [server doc]
  (.add server (solr/make-solr-document (prepare-solr-document doc))))

(defn my-map [docid document]
  (index-document (:server @*config*) document)
  [[(:zip-file-name @*config*) 1]])

(def mapper-map (partial h/standard-map my-map))


;;; MAPPER SHUTDOWN

(defn shutdown-mapper []
  (solr/stop-embedded-solr (:server @*config*)
                           (:core @*config*)))

(defn delete-working-index []
  (FileUtils/deleteDirectory (:work-solr-home @*config*)))

(defn copy-index-to-hdfs-zip-file []
  (let [work-path (FileOutputFormat/getWorkOutputPath (:jobconf @*config*))
        zip-file-path (Path. work-path (:zip-file-name @*config*))
        fs (.getFileSystem zip-file-path (:jobconf @*config*))
        zip-root (File. (:work-solr-home @*config*) "/data/index")]
    (log/info "Copying index to ZIP file on HDFS at " zip-file-path)
    (with-open [out (.create fs zip-file-path)]
      (zip/write-zip-file out (file-seq zip-root) zip-root))))

(defn mapper-close [this]
  (shutdown-mapper)
  (copy-index-to-hdfs-zip-file)
  (delete-working-index))



;;; REDUCER

(defn my-reduce [zip-file-name counts]
  [[zip-file-name (reduce + counts)]])

(def reducer-reduce (partial h/standard-reduce my-reduce))



;;; TOOL METHODS

(defn tool-run [this args]
  (let [job (h/default-jobconf this)
        outpath (Path. (h/job-path :post :distindex))]
    (.setJobName job "distindex")

    (FileInputFormat/setInputPaths job (h/job-path :analyze :merge))
    (FileOutputFormat/setOutputPath job outpath)
    (.delete (FileSystem/get job) outpath true)
    (.setMapperClass job org.altlaw.jobs.post.distindex_mapper)
    (.setReducerClass job org.altlaw.jobs.post.distindex_reducer)
    (JobClient/runJob job))
  0)
