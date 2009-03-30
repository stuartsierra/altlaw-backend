(ns org.altlaw.internal.idserver.KeyResource
  (:gen-class :extends org.restlet.resource.Resource)
  (:require [org.altlaw.internal.idserver.impl :as id]
            [org.altlaw.constants :as const])
  (:import (org.restlet.resource Variant StringRepresentation
                                 ResourceException)
           (org.restlet.data MediaType Status)))

(defn -isModifiable [this] true)

(defn -getVariants [this]
  [(Variant. MediaType/TEXT_PLAIN)])

(defn get-collection-and-key [this]
  (let [attrs (.. this getRequest getAttributes)]
    [(.get attrs "collection") (.get attrs "key")]))

;;GET
(defn -represent [this variant]
  (const/with-context-environment
   (if-let [id (apply id/get-value (get-collection-and-key this))]
     (StringRepresentation. (str id) MediaType/TEXT_PLAIN)
     (throw (ResourceException. Status/CLIENT_ERROR_NOT_FOUND)))))

;;PUT
(defn -storeRepresentation [this entity]
  (const/with-context-environment
   (let [[collection key] (get-collection-and-key this)
         id (Integer/parseInt (.getText entity))]
     (id/set-value collection key id))))

;;POST
(defn -acceptRepresentation [this entity]
  (const/with-context-environment
   (let [[collection key] (get-collection-and-key this)
         new-id (id/find-value collection key)]
     (.. this getResponse
         (setEntity (StringRepresentation. (str new-id)
                                           MediaType/TEXT_PLAIN))))))
;;DELETE
(defn -removeRepresentations [this]
  (const/with-context-environment
   (let [[collection key] (get-collection-and-key this)]
     (id/delete-key collection key))))


