(ns org.altlaw.jobs.procfiles.profed-map
  (:use clojure.contrib.trace org.altlaw.util org.altlaw.util.hadoop)
  (:import (org.altlaw.func PROBodyExtractor Anonymizer
                            PROMetadataReader PROHTMLToText)
           (java.io StringReader))
  (:gen-class
   :extends org.apache.hadoop.mapred.MapReduceBase
   :implements [org.apache.hadoop.mapred.Mapper]))

(import-hadoop)

(def #^Log *log* (LogFactory/getLog "org.altlaw.procfiles.profed"))

(def *docid-map* (ref nil))

(defn process [#^String filename #^"[B" bytes #^Integer size]
  (when-not (.endsWith filename "index.html")
    (if-let [docid-str (@*docid-map* filename)]
      (when-let [docid (try (Integer/parseInt docid-str)
                            (catch NumberFormatException e
                              (.warn *log* (str "Error getting docid for " filename ": " e))))]
        (let [raw-html (Anonymizer/filter (String. bytes 0 size))
              body-html (PROBodyExtractor/filter raw-html)
              body-text (PROHTMLToText/filter body-html)
              meta (PROMetadataReader.)]
          (.setFilename meta filename)
          (.parse meta (StringReader. raw-html))
          {:docid docid
           :doctype "case"
           :files [filename]
           :name (.getTitle meta)
           :dockets (vec (.getDockets meta))
           :citations (vec (.getCitations meta))
           :court (.getCourtId meta)
           :date (.getDate meta) 
           :html body-html
           :text body-text
           :size (count body-text)}))
      (.warn *log* (str "No docid for file " filename)))))

(defn -configure [this job]
  (let [cached (DistributedCache/getLocalCacheFiles job)
        path (first (filter #(= (.getName %) "docids.tsv.gz") cached))]
    (when (nil? path) (throw (RuntimeException. "No docid.tsv.gz in DistributedCache.")))
    (.info *log* (str "Loading docid map from " path))
    (dosync (ref-set *docid-map* (load-tsv-map (str path))))))

(defn -map [self #^Text key #^BytesWritable value
            #^OutputCollector output reporter]
  (let [filename (str key)
        bytes (.get value)]
    (.debug *log* (str "Map input file: " filename))
    (when-let [doc (process filename bytes (.getSize value))]
      (.debug *log* (str "OUTPUT: " (logstr doc)))
      (.collect output (IntWritable. (:docid doc))
                (Text. (pr-str doc))))))
