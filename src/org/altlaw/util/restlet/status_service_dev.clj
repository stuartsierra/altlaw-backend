(ns org.altlaw.util.restlet.status-service-dev
  (:gen-class :extends org.restlet.service.StatusService)
  (:import (org.restlet.data Status MediaType)
           (org.restlet.resource StringRepresentation)))

(defn -getRepresentation [this status request response]
  (when (.isError status)
    (StringRepresentation.
     (with-out-str
       (print "HTTP" (.getCode status) (.getName status) "\n\n")
       (print (.getDescription status) "\n\n")
       (println "Request Method:  " (.getName (.getMethod request)))
       (println "Original Request:" (str (.getOriginalRef request)))
       (println "Target Resource: " (str (.getResourceRef request)))
       (println "Client Accepted Media Types:" (pr-str (.getAcceptedMediaTypes (.getClientInfo request))))
       (when-let [t (.getThrowable status)]
         (.printStackTrace t (java.io.PrintWriter. *out*))))
     MediaType/TEXT_PLAIN)))
