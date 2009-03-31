(ns org.altlaw.internal.privacy.client
  (:require [org.altlaw.util.context :as context]
            [org.altlaw.util.http-client :as client]
            [clojure.contrib.duck-streams :as duck])
  (:import (org.restlet.data Status)))


(defn- get-docid-list [name]
  (let [uri (str (context/internal-uri) "/privacy/docs/" name)
        response (client/http-get uri)]
    (cond
     (.isSuccess (.getStatus response))
     (set (map #(Integer/parseInt %)
               (duck/read-lines (.getReader (.getEntity response)))))

     (= Status/CLIENT_ERROR_NOT_FOUND (.getStatus response))
     #{} ;; return empty set if nothing found

     :else (throw (Exception. (str "HTTP error "
                                   (.getCode (.getStatus response))
                                   " on " uri))))))

(defn get-norobots []
  (get-docid-list "norobots"))

(defn get-removed []
  (get-docid-list "removed"))


(defn- add-docid-list [action-name docids]
  (let [body (with-out-str
               (doseq [d docids] (println d)))
        uri (str (context/internal-uri) "/privacy/docs/" action-name)
        response (client/http-post uri body)]
    (when-not (.isSuccess (.getStatus response))
      (throw (Exception. (str "HTTP error "
                              (.getCode (.getStatus response))
                              " on " uri))))))

(defn add-norobots [docids]
  (add-docid-list "norobots" docids))

(defn add-removed [docids]
  (add-docid-list "removed" docids))

