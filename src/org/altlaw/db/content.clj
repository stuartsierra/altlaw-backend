(ns org.altlaw.db.content
  (:require [org.altlaw.util.s3 :as s3]))

(def #^{:private true} *bucket* "content.altlaw.org")

(defn put-page-bytes [path bytes mime-type]
  (s3/put-object-gzip-bytes *bucket* (str "www/" path) bytes {}))

(defn get-page [path]
  (s3/get-object-string *bucket* (str "www/" path)))
