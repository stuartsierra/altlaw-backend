(ns org.altlaw.test.www.search
  (:require org.altlaw.www.application
            [org.altlaw.util.context :as context]
            [org.altlaw.util.http-client :as client]
            [org.altlaw.util.restlet :as component]
            [clojure.contrib.duck-streams :as duck])
  (:use clojure.contrib.test-is)
  (:import (org.restlet.data MediaType)
           (org.apache.solr.common SolrInputDocument)))

(def *test-documents*
     [{:docid 101
       :doctype "case"
       :name "The Title of Case 101"
       :court "http://id.altlaw.org/courts/us/fed/supreme"
       :date "2001-01-01"
       :text "The quick brown fox jumped over the lazy dog."
       :citations ["101 U.S. 101"]}
      {:docid 102
       :doctype "case"
       :name "The Title of Case 102"
       :court "http://id.altlaw.org/courts/us/fed/app/2"
       :date "2002-02-02"
       :text "The quick brown fox jumped over the lazy dog and chased his tail."
       :citations ["102 F.3d 102"]}
      {:docid 103
       :doctype "case"
       :name "The Title of Case 103"
       :court "http://id.altlaw.org/courts/us/fed/app/3"
       :date "2003-03-03"
       :text "Pack my box with five dozen liquor jugs."
       :citations ["103 F.3d 103"]}])

(defn add-solr-test-docs [solr-server]
  (doseq [doc *test-documents*]
    (let [solrdoc (SolrInputDocument.)]
      (doseq [[k v] doc]
        (.addField solrdoc (name k) v))
      (.add solr-server solrdoc)))
  (.commit solr-server))

(defn server-fixture [f]
  (let [app (new org.altlaw.www.application)]
    (component/with-server
     13665 app
     (binding [client/*client-base-uri* "http://localhost:13665"]
       (let [solr (.. app getContext getAttributes (get "org.altlaw.solr.server"))]
         (when-not (or solr (instance? solr org.apache.solr.client.solrj.SolrServer))
           (throw (Exception. "www application does not have a Solr server")))
         (add-solr-test-docs solr)
         (f))))))

(use-fixtures :once server-fixture)

(deftest can-get-simple-search-page
  (let [response (client/http-get "/v1/search")]
    (is (.isSuccess (.getStatus response)))))

(deftest can-get-boolean-search-page
  (let [response (client/http-get "/v1/search/boolean")]
    (is (.isSuccess (.getStatus response)))))

(deftest can-get-advanced-search-page
  (let [response (client/http-get "/v1/search/advanced")]
    (is (.isSuccess (.getStatus response)))))

(deftest can-do-simple-search
  (let [response (client/http-get "/v1/search?q=dog&command=search+cases")]
    (is (.isSuccess (.getStatus response)))
    (let [content (.getText (.getEntity response))]
      (is (.contains content "The Title of Case 101"))
      (is (.contains content "The Title of Case 102")))))
