(ns org.altlaw.db.docids
  (:require [org.altlaw.db.data :as data]
            [org.altlaw.db.properties :as props]))

(def #^{:private true} *bucket* "altlaw.org")

(defn create-docids [collection]
  (data/create-data (str "docids/" collection) {}))

(defn get-docids [collection]
  (data/get-data (str "docids/" collection)))

(defn get-docid [collection key]
  (get @(get-docids collection) key))

(defn set-docid [collection keys docid]
  (assert (coll? keys))
  (assert (every? string? keys))
  (assert (integer? docid))
  (apply alter (get-docids collection)
         assoc (mapcat list keys (repeat docid))))

(def get-next-docid
     (memoize
      (fn []
        (ref (Integer/parseInt (props/get-property "next_docid"))))))

(defn set-next-docid [docid]
  (dosync (ref-set (get-next-docid) docid)))

(defn- increment-next-docid []
  (alter (get-next-docid) inc))

(defn assign-next-docid [collection keys]
  (dosync (set-docid collection keys @(get-next-docid))
          (increment-next-docid)))

(defn save-next-docid []
  (props/set-property "next_docid" @(get-next-docid)))

(defn save-docids [collection]
  (data/save-data (str "docids/" collection)))

(defn make-docid-request [collection keys]
  {:collection collection, :keys keys})

(defn handle-docid-request [request]
  (assert (contains? request :collection))
  (assert (contains? request :keys))
  (assert (coll? (:keys request)))
  (assign-next-docid (:collection request) (:keys request)))
