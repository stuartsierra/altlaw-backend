(ns org.altlaw.util.http-client
  (:import (org.restlet Client)
           (org.restlet.data Request Protocol MediaType Method)
           (org.restlet.resource StringRepresentation)))

(def *client-base-uri* nil)

(defn http-get [uri]
  (.handle (Client. Protocol/HTTP)
           (Request. Method/GET (str *client-base-uri* uri))))

(defn http-put [uri content]
  (.handle (Client. Protocol/HTTP)
           (Request. Method/PUT (str *client-base-uri* uri)
                     (StringRepresentation. content))))

(defn http-post [uri content]
  (.handle (Client. Protocol/HTTP)
           (Request. Method/POST (str *client-base-uri* uri)
                     (StringRepresentation. content))))

(defn http-delete [uri]
  (.handle (Client. Protocol/HTTP)
           (Request. Method/DELETE (str *client-base-uri* uri))))
