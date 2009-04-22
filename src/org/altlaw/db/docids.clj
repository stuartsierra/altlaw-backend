(ns org.altlaw.db.docids
  (:require [org.altlaw.db.data :as data]))

(def #^{:private true} *bucket* "altlaw.org")

(defn create-docids [collection]
  (data/create-data (str "docids/" collection) {}))

(defn get-docids [collection]
  (data/get-data (str "docids/" collection)))

(defn save-docids [collection]
  (data/save-data (str "docids/" collection)))

(defn get-docid [collection key]
  (get @(get-docids collection) key))

(defn set-docid [collection keys docid]
  (assert (coll? keys))
  (assert (every? string? keys))
  (assert (integer? docid))
  (dosync (apply alter (get-docids collection)
                 assoc (mapcat list keys (repeat docid)))))

