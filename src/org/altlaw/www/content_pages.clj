(ns org.altlaw.www.content-pages
  (:gen-class)
  (:require [org.altlaw.util.files :as files]
            [org.altlaw.util.context :as context])
  (:use org.altlaw.www.render
        org.altlaw.util.markdown
        [clojure.contrib.duck-streams :only (file slurp* spit)]
        [clojure.contrib.test-is :only (with-test is)])
  (import (java.io File)
          (org.apache.commons.lang StringEscapeUtils)))

(with-test
 (defn html-output-file [source-file]
   (let [relpath (files/relative-path (context/www-content-dir) source-file)]
     (file (context/www-public-dir) "/" (.replace relpath ".md" ".html"))))
 (is (= (file (context/www-public-dir) "/foo/index.html")
        (html-output-file (file (context/www-content-dir) "/foo/index.md")))))

(defn gen-markdown-page [markdown-text]
  (let [[contenthead contentbody] (seq (.split markdown-text "---\n" 2))]
    (render :xhtml_page
            :html_title "AltLaw, the free legal search engine"
            :html_head (render :default_html_head
                               :verify true)
            :html_body (render :default_layout
                               :page_class "doctype_about"
                               :content_head (markdown contenthead)
                               :content_body (markdown contentbody)
                               :sidebar (render :sidebar_about_pages)))))

(defn save-static-page [source-file]
  (let [target-file (html-output-file source-file)]
   (.mkdirs (.getParentFile target-file))
   (spit target-file (gen-markdown-page (slurp* source-file)))))

(defn save-static-pages []
  (dorun (map save-static-page
              (filter #(.isFile %) (file-seq (context/www-content-dir))))))

(defn -main [& args]
  (save-static-pages))
