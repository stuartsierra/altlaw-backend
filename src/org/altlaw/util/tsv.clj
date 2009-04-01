;;; tsv.clj: dealing with tab-separated-value (TSV) files

(ns org.altlaw.util.tsv
  (:import (java.io BufferedReader InputStreamReader
                    FileInputStream)
           (java.util.zip GZIPInputStream)))

(defn load-tsv-map
  "Reads a gzip-compressed tab-separated-value file with two columns.
  Returns a map of string=>string."
  [file]
  (with-open [stream (BufferedReader.
                      (InputStreamReader.
                       (GZIPInputStream.
                        (FileInputStream. file))))]
      (reduce (fn [map #^String line]
                  (let [[#^String key value] (.split line "\t")]
                    (assoc map key value)))
              {} (line-seq stream))))

