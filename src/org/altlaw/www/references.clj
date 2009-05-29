(ns org.altlaw.www.references
  (:import (org.restlet.data Reference)))

(defn ref-with-param [ref param-name param-value]
  (let [newref (Reference. ref)
        query (.getQueryAsForm newref)]
    (if (nil? param-value)
      (.removeAll query param-name)
      (.set query param-name (str param-value) true))
    (.setQuery newref (.encode query))
    newref))

(defn page-ref [ref number]
  (ref-with-param ref "page" number))

(defn sort-ref [ref sort]
  (ref-with-param ref "sort" sort))

(defn format-ref [ref format]
  (ref-with-param ref "format" format))