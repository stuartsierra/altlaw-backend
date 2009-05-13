(ns org.altlaw.www.admin.NorobotsResource
  (:gen-class :extends org.restlet.resource.Resource)
  (:require [org.altlaw.db.privacy :as privacy])
  (:import (org.restlet.resource Variant StringRepresentation ResourceException)
           (org.restlet.data Form MediaType Language CharacterSet Reference Status)))


;;; RESOURCE METHODS

(defn -allowGet [this] true)
(defn -allowPost [this] true)
(defn -allowPut [this] false)
(defn -allowDelete [this] false)

(defn -getVariants [this]
  [(Variant. MediaType/TEXT_HTML)])

(defn -acceptRepresentation [this entity]
  (let [params (.getValuesMap (Form. entity))]
    (assert (contains? params "docid"))
    (let [docid (Integer/parseInt (get params "docid"))]
      (privacy/add-norobots [docid])
      (privacy/save-norobots)
      (.. this getResponse
          (setEntity (StringRepresentation.
(str "<html><head><title>Docid No Robots</title></head>
<body>
<h1>Docid No Robots</h1>
<p>Docid " docid " added to norobots.</p>
</body></html>")
                      MediaType/TEXT_HTML
                      Language/ENGLISH_US
                      CharacterSet/UTF_8))))))

(defn -represent [this variant]
  (StringRepresentation.
   (str
"<html><head><title>Docid No Robots</title></head>
<body>
<h1>Docid No Robots</h1>
<form method=\"post\">
<p><label for=\"docid\">Add new norobots Docid</label>
<input name=\"docid\" type=\"text\" size=\"10\" />
<p><input type=\"submit\" value=\"Submit\" /></p>
</p></form>
<h2>Current norobots:</h2>
<p>" (sort @(privacy/get-norobots)) "</p>
</body></html>")
   MediaType/TEXT_HTML
   Language/ENGLISH_US
   CharacterSet/UTF_8))
