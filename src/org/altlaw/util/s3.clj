(ns org.altlaw.util.s3
  (:require [org.altlaw.util.zip :as zip]
            [org.altlaw.util.context :as context]
            [org.altlaw.util.log :as log])
  (:import (org.jets3t.service S3Service)
           (org.jets3t.service.impl.rest.httpclient RestS3Service)
           (org.jets3t.service.model S3Bucket S3Object)
           (org.jets3t.service.security AWSCredentials)
           (org.apache.commons.io IOUtils)
           (org.apache.commons.codec.digest DigestUtils)
           (java.io ByteArrayInputStream)
           (java.util.zip GZIPInputStream)))

(def #^{:private true} get-s3
     (memoize (fn []                 
                (log/debug "Called get-s3 memoized fn")
                (log/info "Using jets3t library version "
                          S3Service/VERSION_NO__JETS3T_TOOLKIT)
                (RestS3Service.
                 (AWSCredentials. (context/aws-access-key-id)
                                  (context/aws-secret-access-key))))))

(def #^{:private true} get-bucket
     (memoize (fn [x]
                (log/debug "Called get-bucket memoized fn on " (pr-str x))
                (if (instance? S3Bucket x) x
                    ;; .getBucket is not supported in jets3t 6.0, included in Hadoop
                    (S3Bucket. x)))))

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

(defn put-object-gzip-bytes
  [bucket-name object-key bytes metadata]
  (let [bucket (get-bucket bucket-name)
        object (S3Object. bucket object-key)
        gzipped-bytes (zip/gzip-bytes bytes)]
    (.addAllMetadata object metadata)
    (.setContentEncoding object "gzip")
    (.setContentLength object (count gzipped-bytes))
    (.setMd5Hash object (DigestUtils/md5 gzipped-bytes))
    (.setDataInputStream object (ByteArrayInputStream. gzipped-bytes))
    (.putObject (get-s3) bucket object)))
