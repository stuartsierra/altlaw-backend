(ns org.altlaw.jobs.pre.ohm1
  (:use org.altlaw.extract.ohm1-utils)
  (:refer clojure.set)
  (:require [org.altlaw.util.hadoop :as h]
            [org.altlaw.util.tsv :as tsv]
            [org.altlaw.util.log :as log]
            [org.altlaw.extract.pdf :as pdf]
            [org.altlaw.util.merge-fields :as merge])
  (:import (org.altlaw.extract Anonymizer Ohm1XMLReader
                               PROHTMLToText)
           (java.io InputStreamReader ByteArrayInputStream)
           (java.util Arrays)))

(h/setup-mapreduce)

;;; MAPPER IMPLEMENTATION

(def *docid-map* (ref nil))

;; Processes a single input file and returns a map of {:field
;; #{values}}. Values are always sets, even when there is only one.
;; Dispatches simply on the MIME-type of the file.  type and filename
;; are Strings; bytes is a byte array; size is the Integer size of
;; that the file content within the array, which may be less than the
;; total length of the array.
(defmulti process (fn [type docid filename bytes size] type))

(defmethod process "application/xml" [type docid filename bytes size]
  (log/debug "process xml: " docid ", " filename ", " size " bytes")
  (h/counter "MIME Types" type)
  (let [reader (InputStreamReader.
                (ByteArrayInputStream. bytes 0 size))
        parser (Ohm1XMLReader.)]
    (.parse parser reader)
    {:docid #{docid}
     :doctype #{"case"}
     :hashes #{(str (MD5Hash/digest bytes 0 size))}
     :files #{filename}
     :name #{(.getName parser)}
     :date #{(.getDate parser)}
     :dockets (set (.getDockets parser))
     :court #{(ohm1-court-uri filename)}}))

(defmethod process "application/pdf" [type docid filename bytes size]
  (log/debug "process PDF: " docid ", " filename ", " size " bytes")
  (h/counter "MIME Types" type)
  (let [bytes (Arrays/copyOf bytes size)]  ; ensure correct size
    (if-let [raw-html (pdf/pdf-to-html filename bytes)]
      (let [html (Anonymizer/filter raw-html)
            text (PROHTMLToText/filterWithTagSoup html)]
        {:docid #{docid}, :doctype #{"case"}
         :hashes #{(str (MD5Hash/digest bytes 0 size))}
         :html #{html}, :text #{text},
         :size #{(count text)}, :files #{filename}})
      (do (h/counter "Failed PDF-to-HTML")
          (log/warn "pdf-to-html failed on " docid ", " filename)))))

(defmethod process :default [type docid filename bytes size]
  (log/warn "process ignoring: " docid ", " filename ", " size " bytes")
  (h/counter "MIME Types" type)
  (h/counter "Ignored files")
  {:docid #{docid}
   :files #{filename}
   :hashes #{(str (MD5Hash/digest bytes 0 size))}})

(defn my-map [filename bytes size]
  (let [hash (str (MD5Hash/digest bytes 0 size))
        type (ohm1-guess-mime-type filename)]
    (if-let [docid (try (Integer/parseInt (@*docid-map* hash))
                        (catch Exception e
                          (h/counter "Bogus docid")
                          (log/warn "Bogus docid for " filename)))]
      (let [doc (process type docid filename bytes size)]
        [[(:docid doc) doc]])
      (do (h/counter "No docid")
          (log/warn "No docid for " filename)))))


;;; MAPPER METHODS

(defn mapper-configure [this job]
  (let [cached (DistributedCache/getLocalCacheFiles job)
        path (first (filter #(= (.getName %) "docids.tsv.gz") cached))]
    (when (nil? path) (throw (RuntimeException. "No docid.tsv.gz in DistributedCache.")))
    (log/info "Loading docid map from " path)
    (dosync (ref-set *docid-map* (tsv/load-tsv-map (str path))))))

(def mapper-map (partial h/byteswritable-map my-map))


;;; REDUCER

(defn my-reduce [docid-set docs]
  (let [doc (reduce merge/singleize {} (apply merge-with union docs))]
    (if (and (:html doc) (:text doc))
      [[(:docid doc) doc]]
      (do (h/counter "No html/text")
          (log/warn "No html/text; skipping " (:docid doc))))))

(def reducer-reduce (partial h/standard-reduce my-reduce))


;;; TOOL

(defn tool-run [this args] []
  (let [job (h/default-jobconf this)
        outpath (Path. (h/job-path :pre :ohm1))
        hdfs (FileSystem/get job)]
    (.setJobName job "pre.ohm1")
    (FileInputFormat/setInputPaths job (str (h/job-path :seqfiles :ohm1) "/*.seq"))
    (FileOutputFormat/setOutputPath job outpath)
    (.delete hdfs outpath true)
    (.setMapperClass job (Class/forName "org.altlaw.jobs.pre.ohm1_mapper"))
    (.setReducerClass job (Class/forName "org.altlaw.jobs.pre.ohm1_reducer"))
    (DistributedCache/addCacheFile (URI. (h/docid-path :seqfiles :ohm1)) job)
    (JobClient/runJob job))
  0)
