(ns org.altlaw.www.admin.NorobotsResource
  (:gen-class :extends org.restlet.resource.Resource)
  (:require [org.altlaw.db.privacy :as privacy]
            [org.altlaw.www.render :as rend])
  (:import (org.restlet.resource Variant StringRepresentation ResourceException)
           (org.restlet.data Form MediaType Language CharacterSet Reference Status)))


;;; RESOURCE METHODS

(defn -allowGet [this] true)
(defn -allowPost [this] true)
(defn -allowPut [this] false)
(defn -allowDelete [this] false)

(defn -getVariants [this]
  [(Variant. MediaType/TEXT_HTML)])

(defn -acceptRepresentation [this entity]
  (let [params (.getValuesMap (Form. entity))]
    (assert (contains? params "docid"))
    (let [docid (Integer/parseInt (get params "docid"))]
      (privacy/add-norobots [docid])
      (privacy/save-norobots)
      (.. this getResponse
          (redirectSeeOther (.. this getRequest getOriginalRef))))))

(defn -represent [this variant]
  (StringRepresentation.
   (rend/render "admin/norobots"
                :norobots (sort @(privacy/get-norobots)))
   MediaType/TEXT_HTML
   Language/ENGLISH_US
   CharacterSet/UTF_8))
