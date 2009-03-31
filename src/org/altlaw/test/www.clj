(ns org.altlaw.test.www
  (:require org.altlaw.www.application
            [org.altlaw.util.context :as context]
            [org.altlaw.util.http-client :as client]
            [org.altlaw.util.restlet :as restlet]
            [clojure.contrib.duck-streams :as duck])
  (:use clojure.contrib.test-is)
  (:import (org.restlet.data MediaType)))

(defn internal-server-fixture [f]
  (restlet/with-server
   13669 (new org.altlaw.www.application)
   (f)))

(defn internal-client-uri-fixture [f]
  (binding [client/*client-base-uri* "http://localhost:13669"]
    (f)))


(use-fixtures :once 
              internal-server-fixture
              internal-client-uri-fixture)

(deftest can-get-static-files
  (let [dirname (name (gensym "test_dir_"))
        filename (name (gensym "test_file_"))
        public-dir (duck/file (context/www-public-dir) "/" dirname)
        public-html-file (duck/file public-dir "/" filename ".html")
        static-dir (duck/file (context/www-static-dir) "/" dirname)
        static-css-file (duck/file static-dir "/" filename ".css")
        html "<html><body>Hello, World!</body></html>"
        css "body { background: blue; }"]

    ;; setup
    (.mkdirs public-dir)
    (.mkdirs static-dir)
    (duck/spit public-html-file html)
    (duck/spit static-css-file css)

    ;; Test that we get the HTML file from /public
    (let [response (client/http-get (str "/" dirname "/" filename ".html"))]
      (is (.isSuccess (.getStatus response)))
      (is (= MediaType/TEXT_HTML (.getMediaType (.getEntity response))))
      (is (= html (.getText (.getEntity response)))))

    ;; Test that we get the CSS file from /static
    (let [response (client/http-get (str "/" dirname "/" filename ".css"))]
      (is (.isSuccess (.getStatus response)))
      (is (= MediaType/TEXT_CSS (.getMediaType (.getEntity response))))
      (is (= css (.getText (.getEntity response)))))
    
    ;; cleanup
    (.delete public-html-file)
    (.delete static-css-file)
    (.delete public-dir)
    (.delete static-dir)))


(deftest can-get-case-text
  (let [docid (str "98765432" (rand-int 10))
        path (str "docs/98/76/54/32/" docid "-text")
        html-file (duck/file (context/www-public-dir) "/" path ".html")
        dir (.getParentFile html-file)
        html "<html><body>can-get-case-file</body></html>"]

    ;; setup
    (.mkdirs dir)
    (duck/spit html-file html)

    ;; Test that we get the HTML file /v1/cases/{docid}
    (let [response (client/http-get (str "/v1/cases/" docid))]
      (is (.isSuccess (.getStatus response)))
      (is (= MediaType/TEXT_HTML (.getMediaType (.getEntity response))))
      (is (= html (.getText (.getEntity response)))))
    
    ;; cleanup
    (.delete html-file)))


(deftest can-get-simple-search-page
  (let [response (client/http-get (str "/v1/search"))]
    (is (.isSuccess (.getStatus response)))))
