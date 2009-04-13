(ns org.altlaw.util.s3
  (:require [org.altlaw.util.zip :as zip])
  (:import (org.jets3t.service.impl.rest.httpclient RestS3Service)
           (org.jets3t.service.model S3Bucket S3Object)
           (org.jets3t.service.security AWSCredentials)
           (org.apache.commons.io IOUtils)
           (java.io ByteArrayInputStream)
           (java.util.zip GZIPInputStream)))

(def #^{:private true} get-s3
     (memoize (fn [] (RestS3Service.
                      (AWSCredentials. (System/getenv "AWS_ACCESS_KEY_ID")
                                       (System/getenv "AWS_SECRET_ACCESS_KEY"))))))

(def #^{:private true} get-bucket
     (memoize (fn [x]
                (if (instance? S3Bucket x) x
                    (.getBucket (get-s3) x)))))

(defn- make-bucket-symbol [bucket]
  (with-meta (symbol (.getName bucket))
             (into {} (.getMetadataMap bucket))))

(defn- make-object-symbol [object]
  (with-meta (symbol (.getKey object))
             (into {} (.getMetadataMap object))))

(defn list-buckets
  "Returns a list of symbols.  Each symbol has a name matching a
  bucket name, and metadata matching that bucket's S3 metadata."
  []
  (map make-bucket-symbol
       (.listAllBuckets (get-s3))))

(defn list-objects
  ([bucket-name]
     (.listObjects (get-s3) (get-bucket bucket-name)))
  ([bucket-name prefix]
     (.listObjects (get-s3) (get-bucket bucket-name) nil)))

(defn delete-object [bucket-name object-key]
  (.deleteObject (get-s3) bucket-name object-key))

(defn get-object [bucket-name object-key]
  (.getObject (get-s3) (get-bucket bucket-name) object-key))

(defn get-object-meta [bucket-name object-key]
  (into {} (.getMetadataMap
            (.getObjectDetails (get-s3) (get-bucket bucket-name)
                               object-key))))

(defn get-object-stream [bucket-name object-key]
  (let [object (get-object bucket-name object-key)
        input (.getDataInputStream object)]
    (if (= (.getContentEncoding object) "gzip")
      (GZIPInputStream. input)
      input)))

(defn get-object-string [bucket-name object-key]
  (IOUtils/toString (get-object-stream bucket-name object-key)
                    "UTF-8"))

(defn put-object-stream [bucket-name object-key stream metadata]
  (let [bucket (get-bucket bucket-name)
        object (S3Object. bucket object-key)]
    (.addAllMetadata object metadata)
    (.setDataInputStream object stream)
    (.putObject (get-s3) bucket object)))

(defn put-object-string [bucket-name object-key data-string metadata]
  (let [bucket (get-bucket bucket-name)
        object (S3Object. bucket object-key data-string)]
    (.addAllMetadata object metadata)
    (.putObject (get-s3) bucket object)))

(defn put-object-gzip-string
  [bucket-name object-key data-string metadata]
  (let [bucket (get-bucket bucket-name)
        object (S3Object. bucket object-key)
        bytes (zip/gzip-utf8-string data-string)]
    (.addAllMetadata object metadata)
    (.setContentEncoding object "gzip")
    (.setContentLength object (count bytes))
    (.setDataInputStream object (ByteArrayInputStream. bytes))
    (.putObject (get-s3) bucket object)))
