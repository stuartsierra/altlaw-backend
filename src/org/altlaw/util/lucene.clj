(ns org.altlaw.util.lucene
  (:import (org.apache.lucene.store Directory FSDirectory)
           (org.apache.lucene.index IndexWriter SerialMergeScheduler)))

(defn merge-indexes
  "Merges Lucene indexes from sources (directory names as Strings) to
  a single index at target (directory name as String)."
  [target sources]
  (let [indexes (map #(FSDirectory/getDirectory %) sources)
        writer (IndexWriter. target nil true)
        index-array (into-array indexes)]
    (.setInfoStream writer (System/err))
    (.setMergeScheduler writer (SerialMergeScheduler.))
    (println (seq index-array))
    (.addIndexes writer index-array)
    ;; addIndexes automatically optimizes the index
    (.commit writer)
    (.close writer)
    (doseq [i indexes] (.close i))))
