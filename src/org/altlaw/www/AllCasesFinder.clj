(ns org.altlaw.www.AllCasesFinder
  (:gen-class :extends org.restlet.Finder)
  (:require org.altlaw.www.SearchResource))


(defn -findTarget [this request response]
  (.. request getResourceRef
      (addQueryParameter "q" "*"))
  (.. request getResourceRef
      (addQueryParameter "sort" "date"))
  (new org.altlaw.www.SearchResource
       (.getContext this) request response))
