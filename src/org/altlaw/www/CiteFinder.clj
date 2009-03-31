(ns org.altlaw.www.CiteFinder
  (:gen-class :extends org.restlet.Finder)
  (:require org.altlaw.www.SearchResource))


(defn -findTarget [this request response]
  (.. request getResourceRef
      (addQueryParameter "q" (.. request getAttributes
                                 (get "citation"))))
  (new org.altlaw.www.SearchResource
       (.getContext this) request response))
