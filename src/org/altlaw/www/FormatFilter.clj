(ns org.altlaw.www.FormatFilter
  (:gen-class :extends org.restlet.Filter)
  (:import (org.restlet Filter)
           (org.restlet.data Preference MediaType)))

(def *accept-atom*
     [(Preference. MediaType/APPLICATION_ATOM_XML)])

(defn -beforeHandle [this request response]
  (let [params (.. request getResourceRef getQueryAsForm)
        format-param (when params (.getFirstValue params "format"))]
    (when (= format-param "atom")
      (.. request getClientInfo (setAcceptedMediaTypes *accept-atom*))))
  Filter/CONTINUE)
