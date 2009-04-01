(ns org.altlaw.jobs.graph-map
    (:gen-class
     :extends org.apache.hadoop.mapred.MapReduceBase
     :implements [org.apache.hadoop.mapred.Mapper
                  org.apache.hadoop.mapred.Reducer])
    (:use org.altlaw.util org.altlaw.util.hadoop)
    (:refer clojure.set))

(import-hadoop)

(def #^Log *log* (LogFactory/getLog "org.altlaw"))

(declare *reporter*)

;; MAP COUNTERS:
;; Owned citations: # of citations that identify cases, may not be unique
;; Citations in text: # of citations used in-line in case text
;;
;; "Owned citations" + "Citations in text"
;; should == "Map output records"
;;
;; REDUCE COUNTERS:
;; Unique cites: # of cites that uniquely identify a single case
;; Non-unique cites: # of cites that point to multiple cases
;; Unmatched cites: # of unique citations found in text with no case to point to
;;
;; "Unmatched cites" + "Non-unique cites" + "Unique cites"
;; should == "Reduce input groups"


(defn map-cite-belongs-to [docid doc]
  (map (fn [cite]
           (.incrCounter *reporter* "graph" "Owned citations" 1)
           [cite {:belongs-to #{docid}}])
       (filter #(re-find #"\d+ (?:F\.2d|F\.3d|U\.S\.) \d+" %)
               (:citations doc))))

(defn map-cites-in-text [docid doc]
  (if (:text doc)
    (let [my-cites (set (:citations doc))]
      (map (fn [cite]
               (.incrCounter *reporter* "graph" "Citations in text" 1)
               [cite {:is-cited-by #{docid}}])
           (filter #(not (my-cites %))
                   (re-seq #"\d+ (?:F\.2d|F\.3d|U\.S\.) \d+"
                           (:text doc)))))
    (.incrCounter *reporter* "graph" "Missing text" 1)))

(defn reduce-cite-links [cite documents]
  (let [result (apply merge-with union documents)
        targets (:belongs-to result)
        c (count targets)]
    (cond
     (= c 1) (let [target (first targets)]
               (.incrCounter *reporter* "graph"
                             "Unique cites" 1)
               (map (fn [source] [source target])
                    (:is-cited-by result)))
     (zero? c) (.incrCounter *reporter* "graph"
                             "Unmatched cites" 1)
     :else (.incrCounter *reporter* "graph"
                         "Non-unique cites" 1))))


;;; map/reduce API implementations

(defn -map [this wdocid wdoc output reporter]
  (let [docid (.get wdocid)
        doc (read-string (str wdoc))]
    (.debug *log* (str "Map input " docid "\t" (logstr doc)))
    (binding [*reporter* reporter]
      (doseq [[key value] (concat (map-cite-belongs-to docid doc)
                                  (map-cites-in-text docid doc))]
        (.debug *log* (str "OUTPUT: " key "\t" (logstr value)))
        (.collect output (Text. key) (Text. (pr-str value)))))))

(defn -reduce [this wcite wdocs output reporter]
  (let [cite (str wcite)
        docs (map (comp read-string str) (iterator-seq wdocs))]
    (.debug *log* (str "Reduce input: " cite "\t" (logstr docs)))
    (binding [*reporter* reporter]
      (doseq [[key value] (reduce-cite-links cite docs)]
        (.debug *log* (str "OUTPUT: " key "\t" value))
        (.collect output (IntWritable. key) (IntWritable. value))))))
