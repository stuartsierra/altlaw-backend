(ns org.altlaw.internal.application
  (:gen-class :extends org.restlet.Application
              :exposes-methods {start superStart
                                stop superStop})
  (:import (org.restlet Router))
  (:use org.altlaw.util.log)
  (:require (org.altlaw.internal.idserver impl CollectionResource KeyResource)
            (org.altlaw.internal.privacy impl DoclistResource)
            [org.altlaw.util.context :as context]))

(defn shutdown-derby []
  (try (java.sql.DriverManager/getConnection
        "jdbc:derby:;shutdown=true")
       ;; Clean shutdown always throws SQLException.
       ;; see http://db.apache.org/derby/papers/DerbyTut/embedded_intro.html#shutdown
       (catch java.sql.SQLException e nil)))

(defn -start [this]
  (.superStart this)
  (info "Starting internal application with "
        (context/altlaw-env) " environment.")
  (org.altlaw.internal.idserver.impl/setup-tables)
  (org.altlaw.internal.privacy.impl/setup-tables)
  (.addShutdownHook (Runtime/getRuntime) (Thread. shutdown-derby)))

(defn -createRoot [this]
  (doto (Router. (.getContext this))
    (.attach "/idserver/{collection}" org.altlaw.internal.idserver.CollectionResource)
    (.attach "/idserver/{collection}/{key}" org.altlaw.internal.idserver.KeyResource)
    (.attach "/privacy/docs/{action}" org.altlaw.internal.privacy.DoclistResource)))

(defn -stop [this]
  (.superStop this)
  (.. this getContext getLogger
      (info "Stopping internal application.")))
