(ns org.altlaw.jobs.link.mergedocs-mapred
    (:gen-class
     :extends org.apache.hadoop.mapred.MapReduceBase
     :implements [org.apache.hadoop.mapred.Reducer])
    (:require [org.altlaw.util.context :as context]
              [org.altlaw.internal.privacy.client :as privacy]
              [org.altlaw.util.merge-fields :as merge])
    (:use org.altlaw.util.hadoop))

(import-hadoop)

(def *log* (LogFactory/getLog "org.altlaw.link.mergedocs"))

(def *removed-docids* (ref #{}))

(defn -configure [this jobconf]
  (binding [context/get-property (fn [name] (.get jobconf name))]
    (dosync (ref-set *removed-docids* (privacy/get-removed)))
    (.info *log* (str "The following docids have been removed: "
                      (pr-str @*removed-docids*)))))

(defn -reduce [this wdocid idocs output reporter]
  (if (@*removed-docids* (.get wdocid))
    (.info *log* (str "Omitting removed docid " (.get wdocid)))
    (let [docs (doall (map (comp read-string str) (iterator-seq idocs)))
          doc (merge/merge-fields docs)]
      ;; (doseq d docs (.debug *log* (str "input: " (pr-str d))))
      ;; (.debug *log* (str "REDUCE OUTPUT: " (pr-str doc)))
      (.collect output wdocid (Text. (pr-str doc))))))
