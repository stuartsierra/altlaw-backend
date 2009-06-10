(ns org.altlaw.www.admin.AdminDocResource
  (:gen-class :extends org.restlet.resource.Resource)
  (:require [org.altlaw.www.DocResource :as doc]
            [org.altlaw.jobs.post.distindex :as index]
            [org.altlaw.db.content :as content]
            [org.altlaw.db.privacy :as privacy]
            [org.altlaw.www.case-pages :as pages]
            [org.altlaw.www.render :as rend]
            [org.altlaw.util.date :as date]
            [org.altlaw.util.courts :as courts]
            [clojure.contrib.stacktrace :as stack])
  (:import (org.restlet.resource Variant StringRepresentation ResourceException)
           (org.restlet.data MediaType Language CharacterSet Reference Status)
           (org.apache.solr.client.solrj SolrQuery)))

(defn print-document [d]
  (doseq [[k v] (dissoc d :text :html)]
    (pr k) (print \tab) (prn v))
  (pr :text) (print \tab) (prn (:text d))
  (pr :html) (print \tab) (prn (:html d)))

(defmulti represent (fn [docid media-type] media-type))

(defmethod represent MediaType/TEXT_HTML [docid media-type]
  (StringRepresentation.
   (with-out-str
    (print-document (doc/prepare-html-document
                     (doc/get-document docid))))
   MediaType/TEXT_PLAIN
   Language/ENGLISH_US
   CharacterSet/UTF_8))


;;; ACCEPTING NEW DOCUMENTS

(defn store [entity]
  (let [data (read-string (.getText entity))]
    (index/index-document doc/*solr* data)
    (content/put-content-string (:html data) {})))


;;; RESOURCE METHODS

(defn -allowGet [this] true)
(defn -allowPut [this] false)
(defn -allowPost [this] true)
(defn -allowDelete [this] false)

(defn -getVariants [this]
  [(Variant. MediaType/TEXT_HTML)])

(defn -represent [this variant]
  (try
   (binding [doc/*solr* (.. this getContext getAttributes (get "org.altlaw.solr.server"))]
     (let [docid (.. this getRequest getAttributes (get "docid"))]
       (represent docid (.getMediaType variant))))
   (catch Exception e
     (if (and (instance? org.jets3t.service.S3ServiceException
                         (stack/root-cause e)))
       (throw (ResourceException. Status/CLIENT_ERROR_NOT_FOUND e))
       (throw e)))))

(defn -acceptRepresentation [this entity]
  (binding [doc/*solr* (.. this getContext getAttributes (get "org.altlaw.solr.server"))]
    (store entity)))

