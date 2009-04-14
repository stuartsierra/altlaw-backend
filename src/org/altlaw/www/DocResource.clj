(ns org.altlaw.www.DocResource
  (:gen-class :extends org.restlet.resource.Resource)
  (:require [org.altlaw.db.content :as content]
            [clojure.contrib.stacktrace :as stack])
  (:import (org.restlet.resource Variant StringRepresentation ResourceException)
           (org.restlet.data MediaType Language CharacterSet Reference Status)))

;;; RESOURCE METHODS

(defn -isModifiable [this] false)

(defn -getVariants [this]
  [(Variant. MediaType/TEXT_HTML)])

(defn -represent [this variant]
  (try
   (let [path (.. this getRequest getOriginalRef (getPath true))
         ;; Truncate the initial "/" from path:
         content (content/get-page (str (.substring path 1) ".html"))]
     (StringRepresentation.
      content
      MediaType/TEXT_HTML Language/ENGLISH_US CharacterSet/UTF_8))
   (catch Exception e
     (if (and (instance? org.jets3t.service.S3ServiceException
                         (stack/root-cause e)))
       (throw (ResourceException. Status/CLIENT_ERROR_NOT_FOUND e))
       (throw e)))))
