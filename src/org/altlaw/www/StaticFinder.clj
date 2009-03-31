;;; org.altlaw.www.StaticFinder

;; This Finder subclass wraps two Directory instances.  If the
;; requested URI can't be found in the first at "ALTLAW_HOME/static",
;; it tries the second, at "ALTLAW_HOME/public".

(ns org.altlaw.www.StaticFinder
  (:gen-class :extends org.restlet.Finder
              :init initialize)
  (:require [org.altlaw.util.context :as context])
  (:import (org.restlet Directory Context)
           (org.restlet.resource Resource)
           (java.io File)
           (java.util.logging Logger)))


(def #^Directory *static-directory*
     (Directory. nil (str "file://"
                          (.getAbsolutePath #^File (context/www-static-dir)))))

(def #^Directory *public-directory*
     (Directory. nil (str "file://"
                          (.getAbsolutePath #^File (context/www-public-dir)))))

(defn set-directory-contexts
  "Directory objects throw NullPointerException without a context.
  Here we set the context provided by the constructor."
  [#^Context context]
  (let [#^Logger logger (.getLogger context)]
    (.fine logger "Called org.altlaw.www.StaticFinder/set-directory-contexts"))
  (.setContext *static-directory* context)
  (.setContext *public-directory* context))

(defn -initialize
  "Constructor override, to set the context based on the Application
  context."
  ([] [[] nil])
  ([context]
     (set-directory-contexts context)
     [[context] nil])
  ([context targetClass]
     (set-directory-contexts context)
     [[context targetClass] nil]))

(defn -findTarget
  "Handle the request, return the result of findTarget on either
  *static-directory* if it has an available resource, otherwise
  defaults to *public-directory*."
  [this request response]
  (let [#^Resource r (.findTarget *static-directory* request response)]
    (if (.isAvailable r)
      ;; The file/directory exists in /static:
      r
      ;; The file/directory does NOT exist in /static:
      (.findTarget *public-directory* request response))))
