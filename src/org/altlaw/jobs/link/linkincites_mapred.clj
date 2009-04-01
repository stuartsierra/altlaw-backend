(ns org.altlaw.jobs.link.linkincites-mapred
    (:gen-class
     :extends org.apache.hadoop.mapred.MapReduceBase
     :implements [org.apache.hadoop.mapred.Mapper
                  org.apache.hadoop.mapred.Reducer])
    (:use org.altlaw.util org.altlaw.util.hadoop)
    (:refer clojure.set))

(import-hadoop)

;; Map step: convert Integer values (from graph stage) into Text.
(defn -map [this wkey wvalue output reporter]
  (.collect output wkey
            (if (instance? Text wvalue)
              wvalue
              (Text. (str wvalue)))))

;; Reduce step: output docid => {:incites ...} pairs
(defn -reduce [this wdocid ivalues output reporter]
  (let [values (doall (map (comp read-string str) (iterator-seq ivalues)))
        cited-docids (filter integer? values)
        citing-doc (select-keys
                    (first (filter (complement integer?) values))
                    [:docid :doctype :citations :court :date :name])]
    (doseq [docid cited-docids]
      (.collect output (IntWritable. docid)
                (Text. (pr-str {:incites #{citing-doc}}))))))
