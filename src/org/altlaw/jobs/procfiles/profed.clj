(ns org.altlaw.jobs.procfiles.profed
  (:require [org.altlaw.util.hadoop :as h]
            [org.altlaw.util.tsv :as tsv]
            [org.altlaw.util.log :as log])
  (:import (org.altlaw.extract PROBodyExtractor Anonymizer
                               PROMetadataReader PROHTMLToText)
           (java.io StringReader)))

(h/setup-mapreduce)

(def *docid-map* (ref nil))

(defn my-map [#^String filename #^"[B" bytes #^Integer size]
  (when-not (.endsWith filename "index.html")
    (if-let [docid-str (@*docid-map* filename)]
      (when-let [docid (try (Integer/parseInt docid-str)
                            (catch NumberFormatException e
                              (h/counter "Bogus docids")
                              (log/warn "Error getting docid for " filename ": " e)))]
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
      (do (h/counter "Files with no docid")
          (log/warn "No docid for file " filename)))))

(defn mapper-configure [this job]
  (let [cached (DistributedCache/getLocalCacheFiles job)
        path (first (filter #(= (.getName %) "docids.tsv.gz") cached))]
    (when (nil? path) (throw (RuntimeException. "No docid.tsv.gz in DistributedCache.")))
    (log/info "Loading docid map from " path)
    (dosync (ref-set *docid-map* (tsv/load-tsv-map (str path))))))

(defn mapper-map [self #^Text wkey #^BytesWritable wvalue
            #^OutputCollector output reporter]
  (let [key (str wkey)
        value (.get wvalue)
        size (.getSize wvalue)]
    (log/debug "Map input: " key " => <" size " bytes>")
    (doseq [[key value] (my-map key value size)]
      (log/debug "Map OUTPUT: " key " => "(log/logstr value))
      (.collect output (Text. (pr-str key))
                (Text. (pr-str value))))))

(defn tool-run [this args] []
  (let [job (h/default-jobconf this)
        outpath (Path. (h/job-path :procfiles :profed))
        hdfs (FileSystem/get job)]
    (.setJobName job "procfiles.profed")
    (FileInputFormat/setInputPaths job (str (h/job-path :seqfiles :profed) "/*.seq"))
    (FileOutputFormat/setOutputPath job outpath)
    (.delete hdfs outpath true)
    (.setMapperClass job (Class/forName "org.altlaw.jobs.procfiles.profed_mapper"))
    (.setNumReduceTasks job 0)
    (DistributedCache/addCacheFile (URI. (h/docid-path :seqfiles :profed)) job)
    (JobClient/runJob job))
  0)

