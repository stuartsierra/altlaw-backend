(ns org.altlaw.www.application
  (:gen-class :extends org.restlet.Application
              :exposes-methods {start superStart
                                stop superStop})
  (:import (org.restlet Router))
  (:require org.altlaw.www.StaticFinder
            org.altlaw.www.CiteFinder
            org.altlaw.www.AllCasesFinder
            org.altlaw.www.SearchResource
            org.altlaw.www.CitationsResource
            org.altlaw.www.DocResource
            org.altlaw.www.content-pages
            [org.altlaw.util.context :as context]
            [org.altlaw.util.solr :as solr]))

(defn -start [this]
  (let [context (.getContext this)
        logger (.getLogger context)]
    (.superStart this)
    (.info logger (str "Starting www application with "
                       (context/altlaw-env) " environment."))
    ;; Generate static content pages:
    (org.altlaw.www.content-pages/save-static-pages)
    ;; Start Solr:
    (let [[server core] (solr/start-embedded-solr (context/solr-home))
          attrs (.getAttributes context)]
      (.put attrs "org.altlaw.solr.server" server)
      (.put attrs "org.altlaw.solr.core" core))))

(defn -stop [this]
  (let [context (.getContext this)
        logger (.getLogger context)]
    (.superStop this)
    (.info logger (str "Stopping www application."))
    (.. context getAttributes (get "org.altlaw.solr.core") close)))

(defn -createRoot [this]
  (let [context (.getContext this)
        static-finder (new org.altlaw.www.StaticFinder context)]
    (doto (Router. context)
      (.attach "/cite/{citation}" (new org.altlaw.www.CiteFinder context))
      (.attach "/v1/cases" (new org.altlaw.www.AllCasesFinder context))
      (.attach "/v1/search/{query_type}" org.altlaw.www.SearchResource)
      (.attach "/v1/search" org.altlaw.www.SearchResource)
      (.attach "/v1/cases/{docid}/citations" org.altlaw.www.CitationsResource)
      (.attach "/v1/cases/{docid}" org.altlaw.www.DocResource)
      (.attach static-finder))))
