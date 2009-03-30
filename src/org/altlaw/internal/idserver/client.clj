(ns org.altlaw.internal.idserver.client
  (:require [org.altlaw.util.http-client :as client]
            [org.altlaw.util.context :as context]
            [clojure.contrib.duck-streams :as duck])
  (:use org.altlaw.util.log)
  (:import (org.restlet.data Status)))


(defn get-value [collection key]
  (let [uri (str (context/internal-uri) "/idserver/" collection "/" key)
        response (client/http-get uri)]
    (cond
     (.isSuccess (.getStatus response))
     (Integer/parseInt (.getText (.getEntity response)))

     (= Status/CLIENT_ERROR_NOT_FOUND (.getStatus response))
     nil

     :else (throw (Exception. (str "idserver error; HTTP status "
                                   (.getCode (.getStatus response))))))))

(defn get-map [collection]
  (let [uri (str (context/internal-uri) "/idserver/" collection)
        response (client/http-get uri)]
    (cond
     (.isSuccess (.getStatus response))
     (with-open [rdr (.getReader (.getEntity response))]
       (doall (map (fn [line] (let [[key value] (.split line "\t")]
                                [key (Integer/parseInt value)]))
                   (duck/read-lines rdr))))

     (= Status/CLIENT_ERROR_NOT_FOUND (.getStatus response))
     nil

     :else (throw (Exception. (str "idserver error; HTTP status "
                                   (.getCode (.getStatus response))))))))

(defn set-value [collection key value]
  (let [uri (str (context/internal-uri) "/idserver/" collection "/" key)
        response (client/http-put uri (str value))]
    (when-not (.isSuccess (.getStatus response))
      (throw (Exception. (str "idserver error; HTTP status "
                              (.getCode (.getStatus response))))))))

(defn find-value [collection key]
  (let [uri (str (context/internal-uri) "/idserver/" collection "/" key)
        response (client/http-post uri "")]
    (cond
     (.isSuccess (.getStatus response))
     (Integer/parseInt (.getText (.getEntity response)))

     :else (throw (Exception. (str "idserver error; HTTP status "
                                   (.getCode (.getStatus response))))))))

(defn delete-key [collection key]
  (let [uri (str (context/internal-uri) "/idserver/" collection "/" key)
        response (client/http-delete uri)]
    (when-not (.isSuccess (.getStatus response))
      (throw (Exception. (str "idserver error; HTTP status "
                              (.getCode (.getStatus response))))))    ))
