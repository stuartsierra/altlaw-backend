(ns org.altlaw.www.admin
  (:require [org.altlaw.util.context :as context]
            org.altlaw.www.admin.NorobotsResource
            org.altlaw.www.admin.MenuResource)
  (:import (org.restlet Router Guard)
           (org.restlet.data ChallengeScheme)))

(defn- make-admin-guard [context next]
  (doto (Guard. context ChallengeScheme/HTTP_DIGEST
                "AltLaw admin")
    (.. getSecrets (put (context/admin-username)
                        (.toCharArray (context/admin-password))))
    (.setNext next)))

(defn- make-admin-router [context]
  (doto (Router. context)
    (.attach "/norobots" org.altlaw.www.admin.NorobotsResource)
    (.attach "" org.altlaw.www.admin.MenuResource)))

(defn make-guarded-admin-router [context]
  (make-admin-guard context (make-admin-router context)))


