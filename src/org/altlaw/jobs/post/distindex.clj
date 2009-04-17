(ns org.altlaw.jobs.post.distindex
   (:require [org.altlaw.util.solr :as solr]
             [org.altlaw.util.hadoop :as hadoop]
             [org.altlaw.util.zip :as zip])
  (:use [clojure.contrib.test-is :only (deftest- is)])
  (:import (org.apache.commons.io FileUtils)
           (java.io File)))

(hadoop/import-hadoop)

(def *log* (LogFactory/getLog "org.altlaw.jobs.post.distindex"))

(gen-class
 :name "org.altlaw.jobs.post.distindex"
 :extends org.apache.hadoop.conf.Configured
 :implements [org.apache.hadoop.util.Tool]
 :prefix "tool-"
 :main true)

(gen-class
 :name "org.altlaw.jobs.post.distindex_mapper"
 :extends org.apache.hadoop.mapred.MapReduceBase
 :implements [org.apache.hadoop.mapred.Mapper]
 :prefix "mapper-"
 :state state
 :init init)



;;; FUNCTIONS USED IN MAPPER

(def *one* (LongWritable. 1))

(defn setup-mapper [taskid index-root]
  (let [work-solr-home (File. index-root taskid)
        zip-file-name (str taskid "-index.zip")]

    (.info *log* (str "Task ID is " taskid))
    (.info *log* (str "Working Solr home is " work-solr-home))
    (.info *log* (str "Index ZIP file name is " zip-file-name))

    ;; Start the embedded Solr server:
    (let [[server core] (solr/start-embedded-solr work-solr-home)]
      ;; Return the configuration state:
      {:taskid taskid
       :work-solr-home work-solr-home
       :zip-file-name zip-file-name
       :zip-file-name-text (Text. zip-file-name)
       :server server
       :core core})))

(defn prepare-solr-document [data]
  (select-keys data [:docid :doctype :name :citations
                     :court :text :size :date]))

(defn index-document [server doc]
  (.add server (solr/make-solr-document (prepare-solr-document doc))))

(defn shutdown-mapper [state]
  (solr/stop-embedded-solr (:server state) (:core state)))

(defn delete-working-index [state]
  (FileUtils/deleteDirectory (:work-solr-home state)))

(defn copy-index-to-hdfs-zip-file [config]
  (let [work-path (FileOutputFormat/getWorkOutputPath (:jobconf config))
        zip-file-path (Path. work-path (:zip-file-name config))
        fs (.getFileSystem zip-file-path (:jobconf config))
        zip-root (File. (:work-solr-home config) "/data/index")]
    (.info *log* (str "Copying index to ZIP file on HDFS at " zip-file-path))
    (with-open [out (.create fs zip-file-path)]
      (zip/write-zip-file out (file-seq zip-root) zip-root))))



;;; MAPPER METHODS

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
    (let [new-state (setup-mapper taskid index-root)]
      (dosync (ref-set (. this state)
                       (assoc new-state :jobconf jobconf))))))


(defn mapper-map [this wdocid wdoc output reporter]
  (.debug *log* (str "mapper got document " wdocid))
  (let [config @(. this state)]
    (index-document (:server config) (read-string (str wdoc)))
    (.debug *log* (str "map output: " (:zip-file-name-text config)
                       "\t" wdocid))
    (.collect output (:zip-file-name-text config) *one*)))

(defn mapper-close [this]
  (let [state @(. this state)]
    (shutdown-mapper state)
    (copy-index-to-hdfs-zip-file state)
    (delete-working-index state)))




;;; TOOL METHODS

(defn tool-run [this args]
  (let [job (hadoop/default-jobconf this)
        outpath (Path. (hadoop/job-path :distindex))]
    (.setJobName job "distindex")

    (FileInputFormat/setInputPaths job (hadoop/job-path :link :mergedocs))
    (FileOutputFormat/setOutputPath job outpath)
    (.delete (FileSystem/get job) outpath true)

    (.setMapperClass
     job (Class/forName "org.altlaw.jobs.post.distindex_mapper"))
    (.setReducerClass job LongSumReducer)
    (.setOutputKeyClass job Text)
    (.setOutputValueClass job LongWritable)
    (.setOutputFormat job TextOutputFormat)
    (FileOutputFormat/setCompressOutput job false)

    (.setNumReduceTasks job 1)
    (JobClient/runJob job))
  0)


(defn tool-main [& args]
  (System/exit
   (ToolRunner/run (Configuration. )
                   (.newInstance (Class/forName "org.altlaw.jobs.post.distindex"))
                   (into-array String args))))



;;; TESTS


(deftest- test-solr
  (let [work-dir (str "/tmp/" (name (gensym "test_work_dir_")))
        taskid (name (gensym "taskid_"))]
    (let [state (setup-mapper taskid work-dir)]
      (is (.exists (File. (str work-dir "/" taskid "/conf"))))
      (is (.exists (File. (str work-dir "/" taskid "/data"))))
      (is (instance? org.apache.solr.client.solrj.embedded.EmbeddedSolrServer
                     (:server state)))

      (index-document (:server state)
                      {:docid 101 :doctype "testdoc"
                       :text "The quick brown fox jumped over the lazy dog."})
      (index-document (:server state)
                      {:docid 102 :doctype "testdoc"
                       :text "Pack my box with five dozen liquor jugs."})
      
      (shutdown-mapper state)
      (is (.isClosed (:core state)))

      (let [indexdir (str work-dir "/" taskid "/data/index")
            searcher (org.apache.lucene.search.IndexSearcher. indexdir)]
        (is (< 1 (.maxDoc searcher)))
        (is (= "The quick brown fox jumped over the lazy dog."
               (.. searcher (doc 0) (get "text"))))
        (.close searcher))

      (delete-working-index state)
      (is (false? (.exists (:work-solr-home state)))))))
