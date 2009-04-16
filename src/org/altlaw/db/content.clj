(ns org.altlaw.db.content
  (:require [org.altlaw.util.s3 :as s3])
  (:import (org.apache.commons.codec.digest DigestUtils)))

(def #^{:private true} *bucket* "content.altlaw.org")

(defn put-page-string [path string mime-type]
  (s3/put-object-gzip-string *bucket* (str "www/" path) string {}))

(defn put-page-bytes [path bytes mime-type]
  (s3/put-object-gzip-bytes *bucket* (str "www/" path) bytes {}))

(defn get-page [path]
  (s3/get-object-string *bucket* (str "www/" path)))

(defn put-content-string [content metadata]
  (let [hash (DigestUtils/shaHex content)
        key (str "sha1/" hash)]
    (when-not (try (s3/get-object-meta *bucket* key)
                   (catch Exception e nil))
      (s3/put-object-gzip-string
       *bucket* key content metadata))
    hash))

(defn put-content-bytes [content metadata]
  (let [hash (DigestUtils/shaHex content)
        key (str "sha1/" hash)]
    (when-not (try (s3/get-object-meta *bucket* key)
                   (catch Exception e nil))
      (s3/put-object-gzip-bytes
       *bucket* key content metadata))
    hash))

(defn get-content-string [hash]
  (let [key (str "sha1/" hash)]
    (s3/get-object-string *bucket* key)))

(defn get-content-bytes [hash]
  (let [key (str "sha1/" hash)]
    (s3/get-object-bytes *bucket* key)))

(defn delete-content [hash]
  (let [key (str "sha1/" hash)]
    (s3/delete-object *bucket* key)))
