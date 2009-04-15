(ns org.altlaw.db.privacy
  (:require [org.altlaw.db.data :as data]
            [clojure.set :as set]))

(defn- norobots-data []
  (data/get-data "norobots"))

(defn create-norobots
  "Creates and initializes the norobots store."
  []
  (data/create-data "norobots" #{}))

(defn add-norobots
  "Adds a collection of document IDs to the norobots store."
  [docids]
  (dosync (commute (norobots-data) set/union (set docids))))

(defn save-norobots
  "Save the norobots store."
  []
  (data/save-data "norobots"))

(defn norobots?
  "Check if the given docid is in the norobots store."
  [docid]
  (contains? @(norobots-data) docid))
