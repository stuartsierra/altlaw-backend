(ns org.altlaw.db.data
  (:require [org.altlaw.util.s3 :as s3]))

(def #^{:private true} *bucket* "content.altlaw.org")

(defn- make-key
  "Transforms a string into an S3 key."
  [string]
  (str "data/" string))

(defn- fetch-data
  "Returns a pair [data, metadata] which should be assigned to the ref
  for this data object."
  [name]
  (let [object (s3/get-object *bucket* (make-key name))
        metadata (s3/get-object-meta object)]
    [(read-string (s3/get-object-string object))
     {:etag (metadata "ETag")}]))

(def #^{:doc "Returns a ref to the data object with the given name."}
     get-data
     (memoize
      (fn [name]
        (let [[data meta] (fetch-data name)]
          (ref data :meta meta)))))

(defn reload-data
  "Reloads data with the given name, altering the current ref and
  destroying any local modifications."
  [name]
  (let [[data meta] (fetch-data name)
        r (get-data name)]
    (alter-meta! r merge meta)
    (dosync (ref-set r data))))

(defn save-data
  "Saves data with the given name.  If the data stored in S3 has
  changed since this data was last loaded, throws an Exception."
  [name]
  (let [path (make-key name)
        metadata (s3/get-object-meta *bucket* path)
        current-etag (metadata "ETag")
        r (get-data name)]
    (if (= current-etag (:etag (meta r)))
      (do (s3/put-object-gzip-string *bucket* path (pr-str @r)
                                     {"Content-Type" "application/clojure"})
          (alter-meta! r assoc :etag (get (s3/get-object-meta *bucket* path) "ETag")))
      (throw (Exception.
              (str "save-data: ETag mismatch, perhaps "
                   "another process saved the same data"))))))

(defn create-data
  "Creates and initializes a data ref with the given name and value;
  does not return it."
  [name value]
  (s3/put-object-gzip-string *bucket* (make-key name)
                             (pr-str value)
                             {"Content-Type" "application/clojure"}))

(defn delete-data
  "Deletes a data object from storage."
  [name]
  (s3/delete-object *bucket* (make-key name)))
