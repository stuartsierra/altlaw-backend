(ns org.altlaw.www.DocResource
  (:gen-class :extends org.restlet.resource.Resource)
  (:require [org.altlaw.db.content :as content])
  (:import (org.restlet.resource Variant StringRepresentation ResourceException)
           (org.restlet.data MediaType Language CharacterSet Reference Status)))

;;; RESOURCE METHODS

(defn -isModifiable [this] false)

(defn -getVariants [this]
  [(Variant. MediaType/TEXT_HTML)])

(defn -represent [this variant]
  (let [path (.. this getRequest getOriginalRef (getPath true))]
    (StringRepresentation.
     ;; Truncate the initial "/" from path:
     (content/get-page (str (.substring path 1) ".html"))
     MediaType/TEXT_HTML Language/ENGLISH_US CharacterSet/UTF_8)))
