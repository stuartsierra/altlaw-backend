(ns org.altlaw.internal.idserver.CollectionResource
  (:gen-class :extends org.restlet.resource.Resource)
  (:require [org.altlaw.internal.idserver.impl :as id]
            [org.altlaw.constants :as const])
  (:import (org.restlet.resource Variant StringRepresentation
                                 ResourceException)
           (org.restlet.data MediaType Status)))

(defn -allowGet [this] true)
(defn -allowPut [this] true)
(defn -allowPost [this] false)
(defn -allowDelete [this] false)

(defn -getVariants [this]
  [(Variant. MediaType/TEXT_PLAIN)])

(defn get-collection [this]
  (.. this getRequest getAttributes (get "collection")))

;;GET
(defn -represent [this variant]
  (const/with-context-environment
   (let [m (id/get-map (get-collection this))]
     (if (seq m)
       (StringRepresentation.
        (with-out-str
          (doseq [[key value] m]
            (print key) (print "\t") (println value)))
        MediaType/TEXT_PLAIN)
       (throw (ResourceException. Status/CLIENT_ERROR_NOT_FOUND))))))

;;PUT
(defn -storeRepresentation [this entity]
  (const/with-context-environment
   (StringRepresentation. "" MediaType/TEXT_PLAIN)))

