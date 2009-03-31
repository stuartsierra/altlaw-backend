;;; org.altlaw.www.DocFilter

;; This Filter subclass 

(ns org.altlaw.www.DocFilter
  (:gen-class :extends org.restlet.Filter)
  (:require [org.altlaw.util.files :as files])
  (:import (org.restlet Filter Context)
           (java.util.logging Logger)))


(defn -beforeHandle
  [this request response]
  (let [attrs (.getAttributes request)
        docid (.get attrs "docid")
        pagetype (or (.get attrs "pagetype") "text")
        path (str (files/docid-path docid) "-" pagetype)
        dirname (subs docid 0 3)
        ref (.getResourceRef request)
        logger (.. this getContext getLogger)]
    (.fine logger (str "DocFilter: got Resource Ref " ref))

    ;; By this point, the "routed" part of the URI already includes
    ;; everything up to and including the docid.  The Directory
    ;; handler is rooted at /public, so we can just add
    ;; /docs/123/123456 onto the Reference.
    (.addSegment ref "docs")
    (doseq [part (.split path "/")] (.addSegment ref part))

    (.fine logger (str "DocFilter: changed Resource Ref to " ref))
    Filter/CONTINUE))
