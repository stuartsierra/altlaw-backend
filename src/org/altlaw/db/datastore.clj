(ns org.altlaw.db.datastore
  (:require [org.altlaw.util.s3 :as s3]))

(def *bucket* "content.altlaw.org")

(defn make-path [path]
  (str "datastore/" path))

(def get-data
     (memoize (fn [path]
                (ref
                 (read-string
                  (s3/get-object-string
                   *bucket* (make-path path)))))))

(defn save-data [path]
  (let [ref (get-data path)]
    (s3/put-object-gzip-string
     *bucket* (make-path path)
     (pr-str @ref) {"Content-Type" "application/clojure"})))

(defn create-data [path]
  (s3/put-object-gzip-string
   *bucket* (make-path path)
   "nil" {"Content-Type" "application/clojure"}))
