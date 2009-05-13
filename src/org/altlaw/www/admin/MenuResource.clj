(ns org.altlaw.www.admin.MenuResource
  (:gen-class :extends org.restlet.resource.Resource)
  (:require [org.altlaw.www.render :as rend])
  (:import (org.restlet.resource Variant StringRepresentation ResourceException)
           (org.restlet.data Form MediaType Language CharacterSet Reference Status)))


;;; RESOURCE METHODS

(defn isModifiable [this] false)

(defn -getVariants [this]
  [(Variant. MediaType/TEXT_HTML)])

(defn -represent [this variant]
  (StringRepresentation.
   (rend/render "admin/index")
   MediaType/TEXT_HTML
   Language/ENGLISH_US
   CharacterSet/UTF_8))
