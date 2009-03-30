(ns org.altlaw.internal.privacy.DoclistResource
  (:gen-class :extends org.restlet.resource.Resource)
  (:require [org.altlaw.constants :as const]
            [org.altlaw.internal.privacy.impl :as impl]
            [clojure.contrib.duck-streams :as duck])
  (:import (org.restlet.resource Variant StringRepresentation
                                 ResourceException)
           (org.restlet.data MediaType Status)))

(defn -allowGet [this] true)
(defn -allowPost [this] true)
(defn -allowPut [this] false)
(defn -allowDelete [this] false)

(defn -getVariants [this]
  [(Variant. MediaType/TEXT_PLAIN)])

(defn- get-action-name [this]
  (.. this getRequest getAttributes (get "action")))

;;GET
(defn -represent [this variant]
  (let [action (get-action-name this)]
    (const/with-context-environment
     (let [m (cond (= action "removed") (impl/get-removed)
                   :else (impl/get-norobots))]
       (if (seq m)
         (StringRepresentation.
          (with-out-str
            (doseq [id m] (println id)))
          MediaType/TEXT_PLAIN)
         (throw (ResourceException. Status/CLIENT_ERROR_NOT_FOUND)))))))

;;POST
(defn -acceptRepresentation [this entity]
  (let [action (get-action-name this)
        f (cond (= action "removed") impl/add-removed
                :else impl/add-norobots)]
    (const/with-context-environment
     (f (map #(Integer/parseInt %)
             (duck/read-lines (.getReader entity)))))))
