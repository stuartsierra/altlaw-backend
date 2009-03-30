(ns org.altlaw.internal.privacy.client
  (:require [org.altlaw.constants :as const]
            [org.altlaw.rest.client :as client]
            [org.altlaw.rest.component :as component]
            org.altlaw.internal.application
            [clojure.contrib.duck-streams :as duck])
  (:use [clojure.contrib.test-is :only (deftest- is test-all-vars)])
  (:import (org.restlet.data Status)))


(defn- get-docid-list [name]
  (let [uri (str const/*internal-base-uri* "/privacy/docs/" name)
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
        uri (str const/*internal-base-uri* "/privacy/docs/" action-name)
        response (client/http-post uri body)]
    (when-not (.isSuccess (.getStatus response))
      (throw (Exception. (str "HTTP error "
                              (.getCode (.getStatus response))
                              " on " uri))))))

(defn add-norobots [docids]
  (add-docid-list "norobots" docids))

(defn add-removed [docids]
  (add-docid-list "removed" docids))


(defn test-ns-hook []
  (const/with-environment
   "testing"
   (component/with-server
    13998 (new org.altlaw.internal.application)
    (binding [const/*internal-base-uri* "http://localhost:13998"]
      (test-all-vars 'org.altlaw.internal.privacy.client)))))

(deftest- can-manage-norobots
  (let [ids (take 3 (repeatedly #(rand-int 1000)))]
    (add-norobots ids)
    (doseq [id ids]
      (is (some #{id} (get-norobots))))))

(deftest- can-manage-removed
  (let [ids (take 3 (repeatedly #(rand-int 1000)))]
    (add-removed ids)
    (doseq [id ids]
      (is (some #{id} (get-removed))))))
