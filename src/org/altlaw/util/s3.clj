(ns org.altlaw.util.s3
  (:import (org.jets3t.service.impl.rest.httpclient RestS3Service)
           (org.jets3t.service.model S3Bucket S3Object)
           (org.jets3t.service.security AWSCredentials)))

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
  (map make-object-symbol
       (.listAllBuckets (get-s3))))

(defn list-objects
  ([bucket-name]
     (.listObjects (get-s3) (get-bucket bucket-name)))
  ([bucket-name prefix]
     (.listObjects (get-s3) (get-bucket bucket-name) nil)))

(defn get-object [bucket-name object-key]
  (.getObject (get-s3) (get-bucket bucket-name) object-key))

(defn get-object-meta [bucket-name object-key]
  (into {} (.getMetadataMap
            (.getObjectDetails (get-s3) (get-bucket bucket-name)
                               object-key))))
