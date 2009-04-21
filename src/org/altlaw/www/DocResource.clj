(ns org.altlaw.www.DocResource
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
                  "date" "court" "html_content_sha1"]))

(defn get-document-query [docid]
  (doto (SolrQuery. (str "docid:" docid))
    (.setQueryType "standard")
    (.setFields *display-fields*)))

(defn get-document
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

(defn prepare-html-document
  "Given a SolrDocument, returns a document map suitable 
  for use by case-pages/gen-case-page"
  [doc]
  {:docid (.getFieldValue doc "docid")
   :name (.getFieldValue doc "name")
   :date (.getFieldValue doc "date")
   :court (.getFieldValue doc "court")
   :citations (.getFieldValues doc "citations")
   :html (content/get-content-string (.getFieldValue doc "html_content_sha1"))})

(defmulti represent (fn [docid media-type] media-type))

(defmethod represent MediaType/TEXT_HTML [docid media-type]
  (StringRepresentation.
   (pages/gen-case-page
    :text (prepare-html-document (get-document docid)))
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
