(ns org.altlaw.jobs.analyze.graph
  (:refer clojure.set)
  (:require [org.altlaw.util.hadoop :as h]
            [org.altlaw.util.log :as log]))

(h/setup-mapreduce)

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
           (h/counter "Owned citations")
           [cite {:belongs-to #{docid}}])
       (filter #(re-find #"\d+ (?:F\.2d|F\.3d|U\.S\.) \d+" %)
               (:citations doc))))

(defn map-cites-in-text [docid doc]
  (if (:text doc)
    (let [my-cites (set (:citations doc))]
      (map (fn [cite]
               (h/counter "Citations in text")
               [cite {:is-cited-by #{docid}}])
           (filter #(not (my-cites %))
                   (re-seq #"\d+ (?:F\.2d|F\.3d|U\.S\.) \d+"
                           (:text doc)))))
    (h/counter "Missing text")))

(defn my-reduce [cite documents]
  (let [result (apply merge-with union documents)
        targets (:belongs-to result)
        c (count targets)]
    (cond
     (= c 1) (let [target (first targets)]
               (h/counter "Unique cites")
               (map (fn [source] [source target])
                    (:is-cited-by result)))
     (zero? c) (h/counter "Unmatched cites")
     :else (h/counter "Non-unique cites"))))


(defn my-map [docid doc]
  (concat (map-cite-belongs-to docid doc)
          (map-cites-in-text docid doc)))


;;; METHOD IMPLEMENTATIONS

(def mapper-map (partial h/standard-map my-map))

(def reducer-reduce (partial h/standard-reduce my-reduce))

(defn tool-run [this args] []
  (let [job (h/default-jobconf this)
        outpath (Path. (h/job-path :analyze :graph))
        hdfs (FileSystem/get job)]
    (.setJobName job "graph")
    (FileInputFormat/setInputPaths job (str (h/job-path :pre :profed)
                                            "," (h/job-path :pre :ohm1)))
    (FileOutputFormat/setOutputPath job outpath)
    (.delete hdfs outpath true)
    (.setMapperClass job org.altlaw.jobs.analyze.graph_mapper)
    (.setReducerClass job org.altlaw.jobs.analyze.graph_reducer)
    (JobClient/runJob job))
  0)
