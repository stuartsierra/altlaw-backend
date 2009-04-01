(ns org.altlaw.jobs.procfiles.ohm1-map
    (:gen-class
     :extends org.apache.hadoop.mapred.MapReduceBase
     :implements [org.apache.hadoop.mapred.Mapper
                  org.apache.hadoop.mapred.Reducer])
    (:use org.altlaw.util org.altlaw.func.ohm1 org.altlaw.util.hadoop)
    (:refer clojure.set)
    (:import (java.io InputStreamReader ByteArrayInputStream File)
             (java.util Arrays)
             (org.altlaw.util RandomWait)
             (org.altlaw.func Ohm1XMLReader RunProgramOnFile
                              PROHTMLToText Anonymizer)))

(import-hadoop)

(def #^Log *log* (LogFactory/getLog "org.altlaw.procfiles.ohm1"))

(def *docid-map* (ref nil))

(def *pdf-to-html-executable* (File. "/usr/local/bin/altlaw_parse_pdf"))
(def *runner* (RunProgramOnFile.))
(.setExecutable *runner* *pdf-to-html-executable*)

(declare *reporter*)

(defn pdf-to-html [filename bytes]
  (try 
   (String. (.exec *runner* bytes))
   (catch org.apache.commons.exec.ExecuteException e
     (.warn *log* (str "altlaw_parse_pdf failed on " filename ": " e))
     nil)))

;; Processes a single input file and returns a map of {:field
;; #{values}}. Values are always sets, even when there is only one.
;; Dispatches simply on the MIME-type of the file.  type and filename
;; are Strings; bytes is a byte array; size is the Integer size of
;; that the file content within the array, which may be less than the
;; total length of the array.
(defmulti process (fn [type docid filename bytes size] type))

(defmethod process "application/xml" [type docid filename bytes size]
  (.debug *log* (str "Processing " docid "; " filename "; " size " bytes"))
  (.incrCounter *reporter* "procfiles.ohm1" (str "Seen type " type) 1)
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
  (.debug *log* (str "Processing " docid "; " filename "; " size " bytes"))
  (.incrCounter *reporter* "procfiles.ohm1" (str "Seen type " type) 1)
  (let [bytes (Arrays/copyOf bytes size)]  ; ensure correct size
    (if-let [raw-html (pdf-to-html filename bytes)]
      (let [html (Anonymizer/filter raw-html)
            text (PROHTMLToText/filterWithTagSoup html)]
        {:docid #{docid}, :doctype #{"case"}
         :hashes #{(str (MD5Hash/digest bytes 0 size))}
         :html #{html}, :text #{text},
         :size #{(count text)}, :files #{filename}})
      (.incrCounter *reporter* "procfiles.ohm1" "Failed PDF-to-HTML" 1))))

(defmethod process :default [type docid filename bytes size]
  (.debug *log* (str "Ignoring " docid "; " filename "; " size " bytes"))
  (.incrCounter *reporter* "procfiles.ohm1" (str "Seen type " type) 1)
  (.incrCounter *reporter* "procfiles.ohm1" "Ignored files" 1)
  {:docid #{docid}
   :files #{filename}
   :hashes #{(str (MD5Hash/digest bytes 0 size))}})

(defn -configure [this job]
  (let [cached (DistributedCache/getLocalCacheFiles job)
        path (first (filter #(= (.getName %) "docids.tsv.gz") cached))]
    (when (nil? path) (throw (RuntimeException. "No docid.tsv.gz in DistributedCache.")))
    (.info *log* (str "Loading docid map from " path))
    (dosync (ref-set *docid-map* (load-tsv-map (str path))))))

(defn -map [this wfilename wbytes output reporter]
  (let [filename (str wfilename)
        bytes (.get wbytes)
        size (.getSize wbytes)
        hash (str (MD5Hash/digest bytes 0 size))
        type (ohm1-guess-mime-type filename)]
    (if-let [docid (try (Integer/parseInt (@*docid-map* hash))
                        (catch Exception e
                          (.warn *log* (str "Failed to get docid for "
                                            filename ": " e))))]
        (when-let [doc (binding [*reporter* reporter]
                         (process type docid filename bytes size))]
            (.debug *log* (str "OUTPUT: " (logstr doc)))
          (.collect output (IntWritable. docid) (Text. (pr-str doc))))
        (.incrCounter reporter "procfiles.ohm1" "No docid" 1))))

(defn -reduce [this wdocid wfields output reporter]
  (.debug *log* (str "Reduce processing " (.get wdocid)))
  (let [docid (.get wdocid)
        maps (doall (map (comp read-string str) (iterator-seq wfields)))
        doc (reduce singleize {} (apply merge-with union maps))]
    (if (and (:html doc) (:text doc))
      (do (.debug *log* (str "OUTPUT:" (logstr doc)))
          (.collect output wdocid (Text. (pr-str doc))))
      ;; else
      (do (.warn *log* (str docid ": No html/text; skipping."))
          (.incrCounter reporter "procfiles.ohm1" "No text" 1)))))
