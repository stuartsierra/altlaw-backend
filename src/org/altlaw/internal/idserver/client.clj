(ns org.altlaw.internal.idserver.client
  (:require [org.altlaw.rest.client :as client]
            [org.altlaw.constants :as const]
            [org.altlaw.rest.component :as component]
            [clojure.contrib.duck-streams :as duck]
            org.altlaw.internal.application)
  (:use [clojure.contrib.test-is :only (deftest- is test-all-vars)])
  (:import (org.restlet.data Status)))


(defn get-value [collection key]
  (let [uri (str const/*internal-base-uri* "/idserver/" collection "/" key)
        response (client/http-get uri)]
    (cond
     (.isSuccess (.getStatus response))
     (Integer/parseInt (.getText (.getEntity response)))

     (= Status/CLIENT_ERROR_NOT_FOUND (.getStatus response))
     nil

     :else (throw (Exception. (str "idserver error; HTTP status "
                                   (.getCode (.getStatus response))))))))

(defn get-map [collection]
  (let [uri (str const/*internal-base-uri* "/idserver/" collection)
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
  (let [uri (str const/*internal-base-uri* "/idserver/" collection "/" key)
        response (client/http-put uri (str value))]
    (when-not (.isSuccess (.getStatus response))
      (throw (Exception. (str "idserver error; HTTP status "
                              (.getCode (.getStatus response))))))))

(defn find-value [collection key]
  (let [uri (str const/*internal-base-uri* "/idserver/" collection "/" key)
        response (client/http-post uri "")]
    (cond
     (.isSuccess (.getStatus response))
     (Integer/parseInt (.getText (.getEntity response)))

     :else (throw (Exception. (str "idserver error; HTTP status "
                                   (.getCode (.getStatus response))))))))

(defn delete-key [collection key]
  (let [uri (str const/*internal-base-uri* "/idserver/" collection "/" key)
        response (client/http-delete uri)]
    (when-not (.isSuccess (.getStatus response))
      (throw (Exception. (str "idserver error; HTTP status "
                              (.getCode (.getStatus response))))))    ))



(defn test-ns-hook []
  (const/with-environment
   "testing"
   (component/with-server
    13998 (new org.altlaw.internal.application)
    (binding [const/*internal-base-uri* "http://localhost:13998"]
      (test-all-vars 'org.altlaw.internal.idserver.client)))))

(deftest- can-set-and-get-new-values
  (let [coll (name (gensym "coll"))
        value (rand-int Integer/MAX_VALUE)]
    (set-value coll "one" value)
    (is (= value (get-value coll "one")))))

(deftest- can-find-value
  (let [coll (name (gensym "coll"))]
    (find-value coll "foo")
    (is (integer? (get-value coll "foo")))))

(deftest- find-value-returns-values-that-already-exist
  (let [coll (name (gensym "coll"))
        value (rand-int Integer/MAX_VALUE)]
    (set-value coll "foo" value)
    (is (= value (get-value coll "foo")))
    (is (= value (find-value coll "foo")))
    (is (= value (get-value coll "foo")))))

(deftest- nonexistent-values-return-nil
  (let [coll (name (gensym "coll"))]
    (is (nil? (get-value coll "does-not-exist")))))

(deftest- deleted-keys-return-nil
  (let [coll (name (gensym "coll"))
        value (rand-int Integer/MAX_VALUE)]
    (set-value coll "one" value)
    (is (= value (get-value coll "one")))
    (delete-key coll "one")
    (is (nil? (get-value coll "one")))))

(deftest- can-get-entire-collection
  (let [coll (name (gensym "coll"))
        value1 (rand-int Integer/MAX_VALUE)
        value2 (rand-int Integer/MAX_VALUE)
        value3 (rand-int Integer/MAX_VALUE)
        correct {"one" value1, "two" value2,
                 "three" value3}]
    (set-value coll "one" value1)
    (set-value coll "two" value2)
    (set-value coll "three" value3)
    (is (= correct (into {} (get-map coll))))))
