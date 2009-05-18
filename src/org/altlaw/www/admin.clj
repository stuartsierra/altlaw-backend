(ns org.altlaw.www.admin
  (:require [org.altlaw.util.context :as context]
            org.altlaw.www.admin.NorobotsResource
            org.altlaw.www.admin.MenuResource
            [clojure.contrib.shell-out :as sh])
  (:import (org.restlet Router Guard Directory)
           (org.restlet.data ChallengeScheme)
           (java.io File)))

(defn- make-admin-guard [context next]
  (doto (Guard. context ChallengeScheme/HTTP_DIGEST
                "AltLaw admin")
    (.. getSecrets (put (context/admin-username)
                        (.toCharArray (context/admin-password))))
    (.setNext next)))

(defn- make-fn-restlet [context f]
  (proxy [org.restlet.Restlet] [context]
    (handle [request response]
            (.start (Thread. f))
            (.redirectSeeOther response "/admin"))))

(defn- sh-in-thread [args]
  (apply sh/sh (concat args [:dir (str (context/altlaw-home))])))

(defn- make-command-restlet [context args]
  (make-fn-restlet context (fn [] (sh-in-thread args))))

(defn- logs-uri []
  (str "file://" (.getAbsolutePath (File. (context/altlaw-home)))
       "/var/log/" (context/altlaw-env)))

(defn- make-admin-router [context]
  (doto (Router. context)
    (.attach "/norobots" org.altlaw.www.admin.NorobotsResource)
    (.attach "/update-code" (make-command-restlet context ["git" "pull"]))
    (.attach "/recompile" (make-command-restlet context ["ant"]))
    (.attach "/make-content-pages" (make-fn-restlet context org.altlaw.www.content-pages/save-static-pages))
    (.attach "/logs" (doto (Directory. context (logs-uri))
                       (.setListingAllowed true)))
    (.attach "" org.altlaw.www.admin.MenuResource)))

(defn make-guarded-admin-router [context]
  (make-admin-guard context (make-admin-router context)))


