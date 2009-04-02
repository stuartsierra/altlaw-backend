(ns org.altlaw.db.docids
  (:require [org.altlaw.util.simpledb :as db]))

;;; PRIVATE

(def #^{:private true} *domain* "Docids")

(defn- make-db-key [collection-name collection-key]
  (let [s (str collection-name ":" collection-key)]
    ;; Ensure the key fits within SimpleDB's 1024-byte limit
    (if (< (count s) 1024)
      s (subs s 0 1024))))

(def #^{:private true} *number-format* "%010d")

(def #^{:private true} *base-number-string*
     (format *number-format* 10000))

;;; PUBLIC

(defn get-docid [collection key]
  (when-let [string (get (db/get-attrs *domain*
                                     (make-db-key collection key))
                       "docid")]
    ;; Integer/parseInt ignores leading zeros
    (Integer/parseInt string)))

(defn put-docid [collection key docid]
  (db/put-attrs *domain* (make-db-key collection key)
                {:collection collection
                 :key key
                 ;; Add leading zeros for lexicographic ordering
                 :docid (format *number-format* docid)}))

(defn delete-docid [collection key]
  (db/delete-item *domain* (make-db-key collection key)))

(defn max-docid []
  (let [result (db/select-items
                *domain*
                (str "select docid from Docids where docid > "
                     \' *base-number-string* \'
                     " order by docid desc limit 1"))]
    (Integer/parseInt (get (first (vals result)) "docid"))))

