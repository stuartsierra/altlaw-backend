(ns org.altlaw.util.files
  (:use [clojure.contrib.test-is :only (with-test is)])
  (:import (java.io File)))

(defn relative-path
  "Returns the path of file relative to dir, as a String.  The file
  must be in a directory somewhere under dir."
  [#^File dir #^File file]
  (let [dirpath (.getAbsolutePath dir)
        filepath (.getAbsolutePath file)]
    (if (.startsWith filepath dirpath)
      (.replace filepath (str dirpath File/separatorChar) "")
      (throw (Exception. (str filepath " is not relative to " dirpath))))))

(with-test
    (defn docid-path
      "Returns the multi-level relative path for a document id.
      E.g., docid 1234567 becomes 12/34/56/1234567"
      [docid]
      (let [id (str docid)
            dirs (map #(apply str %) (partition 2 id))
            dir (apply str (interpose "/" (if (even? (count id))
                                            (butlast dirs) dirs)))]
        (str dir "/" id)))
  ;; test
  (is (= "12/1234" (docid-path 1234)))
  (is (= "12/34/123456" (docid-path 123456)))
  (is (= "12/34/56/1234567" (docid-path 1234567)))
  (is (= "12/34/56/78/123456789" (docid-path 123456789))))

(def #^{:private true} *extension-mime-types*
  {"txt"   "text/plain"
   "asc"   "text/plain"
   "htm"   "text/html"
   "html"  "text/html"
   "json"  "application/json"
   "xml"   "application/xml"
   "xhtml" "application/xhtml+xml"
   "pdf"   "application/pdf"
   "doc"   "application/msword"
   "wpd"   "application/vnd.wordperfect"})

(defn guess-mime-type-by-name [#^File file]
  (let [extension (.toLowerCase #^String (last (.split (str file) "\\.")))]
    (*extension-mime-types* extension)))

