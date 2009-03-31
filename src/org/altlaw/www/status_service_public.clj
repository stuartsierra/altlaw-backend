(ns org.altlaw.www.status-service-public
  (:require [org.altlaw.www.render :as tmpl])
  (:gen-class :extends org.restlet.service.StatusService)
  (:import (org.restlet.data Status MediaType)
           (org.restlet.resource StringRepresentation)))

(defn html-error [message]
  (tmpl/render :xhtml_page
               :html_title "Error - AltLaw"
               :html_head (tmpl/render :default_html_head)
               :html_body (tmpl/render :default_layout
                                       :page_class "doctype_about"
                                       :content_head "<h1>Error</h1>"
                                       :content_body (tmpl/render :error
                                                                  :message message))))

(defn html-notfound [url]
  (tmpl/render :xhtml_page
               :html_title "Page Not Found - AltLaw"
               :html_head (tmpl/render :default_html_head)
               :html_body (tmpl/render :default_layout
                                       :page_class "doctype_about"
                                       :content_head "<h1>Page Not Found</h1>"
                                       :content_body (tmpl/render :notfound
                                                                  :message (str "HTTP 404 Not Found: " url)))))

(defn -getRepresentation [this status request response]
  (when (.isError status)
    (StringRepresentation.
     (cond (= Status/CLIENT_ERROR_NOT_FOUND status)
           (html-notfound (.getOriginalRef request))
           :else
           (html-error (str status)))
     MediaType/TEXT_HTML)))
