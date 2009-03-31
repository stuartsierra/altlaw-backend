(ns org.altlaw.test.internal
  (:require [org.altlaw.util.context :as context]
            [org.altlaw.util.http-client :as client]
            [org.altlaw.util.restlet :as restlet]
            [clojure.contrib.duck-streams :as duck])
  (:use clojure.contrib.test-is))

(defn internal-server-fixture [f]
  (restlet/with-server
   13669 (new org.altlaw.internal.application)
   (f)))

(defn internal-client-uri-fixture [f]
  (binding [client/*client-base-uri* "http://localhost:13669"]
    (f)))

(defn internal-uri-fixture [f]
  (binding [context/internal-uri (fn [] "http://localhost:13669")]
    (f)))

(use-fixtures :once 
              internal-server-fixture
              internal-client-uri-fixture)


(deftest can-store-and-retrieve-ids
  (let [response (client/http-put "/idserver/testcoll/key1" "101")]
    (is (.isSuccess (.getStatus response))))
  (let [response (client/http-get "/idserver/testcoll/key1")]
    (is (.isSuccess (.getStatus response)))
    (is (= "101" (.getText (.getEntity response)))))
  (let [response (client/http-delete "/idserver/testcoll/key1")]
    (is (.isSuccess (.getStatus response))))
  (let [response (client/http-get "/idserver/testcoll/key1")]
    (is (= org.restlet.data.Status/CLIENT_ERROR_NOT_FOUND
           (.getStatus response)))))

(deftest can-get-new-ids
  (let [path (str "/idserver/testcoll/" (gensym "key"))
        response (client/http-post path "")]
    (is (.isSuccess (.getStatus response)))
    (is (integer? (Integer/parseInt (.getText (.getEntity response)))))
    (client/http-delete path)))

(deftest can-get-id-collection
  (let [path1 "/idserver/testcoll/key1"
        path2 "/idserver/testcoll/key2"]
    (client/http-put path1 "1001")
    (client/http-put path2 "1002")
    (let [response (client/http-get "/idserver/testcoll")]
      (is (.isSuccess (.getStatus response)))
      (is (= "key1\t1001\nkey2\t1002\n"
             (.getText (.getEntity response)))))
    (client/http-delete path1)
    (client/http-delete path2)))


(deftest can-post-and-get-removed-list
  (let [number (rand-int 10000)]
    (let [response (client/http-post "/privacy/docs/removed" (str number))]
      (is (.isSuccess (.getStatus response))))
    (let [response (client/http-get "/privacy/docs/removed")]
      (is (.isSuccess (.getStatus response)))
      (is (.contains (.getText (.getEntity response)) (str number "\n"))))))

(deftest can-post-and-get-norobots-list
  (let [number (rand-int 10000)]
    (let [response (client/http-post "/privacy/docs/norobots" (str number))]
      (is (.isSuccess (.getStatus response))))
    (let [response (client/http-get "/privacy/docs/norobots")]
      (is (.isSuccess (.getStatus response)))
      (is (.contains (.getText (.getEntity response)) (str number "\n"))))))
