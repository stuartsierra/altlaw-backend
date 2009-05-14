(ns org.altlaw.www.status-service-public
  (:require [org.altlaw.www.render :as tmpl]
            [org.altlaw.util.restlet.error-log :as errlog])
  (:gen-class :extends org.restlet.service.StatusService)
  (:import (org.restlet.data Status MediaType CharacterSet Language)
           (org.restlet.resource StringRepresentation)))

(defn html-error [message]
  (tmpl/render :default_layout
               :html_title "Error - AltLaw"
               :page_class "doctype_about"
               :content_head "<h1>Error</h1>"
               :content_body (tmpl/render :error
                                          :error_message message)))

(defn html-notfound [url]
  (tmpl/render :default_layout
               :html_title "Page Not Found - AltLaw"
               :page_class "doctype_about"
               :content_head "<h1>Page Not Found</h1>"
               :content_body (tmpl/render :notfound
                                          :error_message (str "HTTP 404 Not Found: " url))))

(defn -getRepresentation [this status request response]
  (when (.isError status)
    (errlog/log-error-status status)
    (StringRepresentation.
     (cond
       (= Status/CLIENT_ERROR_NOT_FOUND status) (html-notfound (.getOriginalRef request))
       :else (html-error (str status)))
     MediaType/TEXT_HTML
     Language/ENGLISH_US
     CharacterSet/UTF_8)))
