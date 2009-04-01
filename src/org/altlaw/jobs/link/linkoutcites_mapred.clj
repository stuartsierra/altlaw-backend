(ns org.altlaw.jobs.link.linkoutcites-mapred
    (:gen-class
     :extends org.apache.hadoop.mapred.MapReduceBase
     :implements [org.apache.hadoop.mapred.Mapper
                  org.apache.hadoop.mapred.Reducer])
    (:use org.altlaw.util org.altlaw.util.hadoop)
    (:refer clojure.set))

(import-hadoop)

(defn -map [this wkey wvalue output reporter]
  (if (instance? Text wvalue)
    ;; Output documents normally
    (.collect output wkey wvalue)
    ;; Invert key-value mapping for VIntWritables (cite links)
    (.collect output wvalue (Text. (str wkey)))))

(defn -reduce [this wdocid ivalues output reporter]
  (let [values (doall (map (comp read-string str) (iterator-seq ivalues)))
        cited-docids (filter integer? values)
        citing-doc (select-keys
                    (first (filter (complement integer?) values))
                    [:docid :doctype :citations :court :date :name])]
    (doseq [docid cited-docids]
      (.collect output (IntWritable. docid)
                (Text. (pr-str {:outcites #{citing-doc}}))))))
