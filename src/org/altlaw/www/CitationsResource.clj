(ns org.altlaw.www.CitationsResource
  (:gen-class :extends org.restlet.resource.Resource)
  (:require [org.altlaw.db.content :as content]
            [org.altlaw.db.privacy :as privacy]
            [org.altlaw.www.case-pages :as pages]
            [org.altlaw.www.render :as rend]
            [org.altlaw.util.date :as date]
            [org.altlaw.util.courts :as courts]
            [clojure.contrib.stacktrace :as stack])
  (:import (org.restlet.resource Variant StringRepresentation ResourceException)
           (org.restlet.data MediaType Language CharacterSet Reference Status)
           (org.apache.solr.client.solrj SolrQuery)))


(declare *solr*)

(def *display-fields*
     (into-array ["doctype" "docid" "name" "citations"
                  "date" "court" "size"]))

(def *cite-fields*
     (into-array ["doctype" "docid" "name" "citations"
                  "date" "court"]))

(defn get-document-query [docid]
  (doto (SolrQuery. (str "docid:" docid))
    (.setQueryType "standard")
    (.setFields *display-fields*)))

(defn get-incites-query [docid]
  (doto (SolrQuery. (str "incites:" docid))
    (.setQueryType "standard")
    (.setFields *cite-fields*)
    (.setRows 100)))

(defn get-outcites-query [docid]
  (doto (SolrQuery. (str "outcites:" docid))
    (.setQueryType "standard")
    (.setFields *cite-fields*)
    (.setRows 100)))

(defn get-displayed-document
  "Searches Solr for the document with the given docid.
  Returns a SolrDocument.
  Throws ResourceException 404 if not found, or 410 if removed."
  [docid]
  (if (privacy/removed? docid)
    (throw (ResourceException. Status/CLIENT_ERROR_GONE))
    (let [response (.query *solr* (get-document-query docid))
          results (.getResults response)]
      (if (zero? (.getNumFound results))
        (throw (ResourceException. Status/CLIENT_ERROR_NOT_FOUND))
        (first results)))))

(defn get-cite-documents
  "Searches Solr for the documents citing/cited by the given docid.
  Returns a vector pair of SolrDocumentList objects; incites and outcites."
  [docid]
  [(.getResults (.query *solr* (get-incites-query docid)))
   (.getResults (.query *solr* (get-outcites-query docid)))])

(defn prepare-cite-document
  [doc]
  {:docid (.getFieldValue doc "docid")
   :name (.getFieldValue doc "name")
   :date (.getFieldValue doc "date")
   :court (.getFieldValue doc "court")
   :citations (.getFieldValues doc "citations")})

(defn prepare-displayed-document
  "Given a SolrDocument, returns a document map suitable 
  for use by case-pages/gen-case-page"
  [doc incite-docs outcite-docs]
  {:docid (.getFieldValue doc "docid")
   :name (.getFieldValue doc "name")
   :date (.getFieldValue doc "date")
   :court (.getFieldValue doc "court")
   :citations (.getFieldValues doc "citations")
   :incites (map prepare-cite-document outcite-docs)
   :outcites (map prepare-cite-document incite-docs)})

(defmulti represent (fn [docid media-type] media-type))

(defmethod represent MediaType/TEXT_HTML [docid media-type]
  (StringRepresentation.
   (pages/gen-case-page
    :citations (let [[incites outcites] (get-cite-documents docid)]
                 (prepare-displayed-document (get-displayed-document docid)
                                             incites outcites)))
   MediaType/TEXT_HTML
   Language/ENGLISH_US
   CharacterSet/UTF_8))

;;; RESOURCE METHODS

(defn -isModifiable [this] false)

(defn -getVariants [this]
  [(Variant. MediaType/TEXT_HTML)])

(defn -represent [this variant]
  (try
   (binding [*solr* (.. this getContext getAttributes (get "org.altlaw.solr.server"))]
     (let [docid (.. this getRequest getAttributes (get "docid"))]
       (represent docid (.getMediaType variant))))
   (catch Exception e
     (if (and (instance? org.jets3t.service.S3ServiceException
                         (stack/root-cause e)))
       (throw (ResourceException. Status/CLIENT_ERROR_NOT_FOUND e))
       (throw e)))))
