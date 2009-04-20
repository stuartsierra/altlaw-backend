(ns org.altlaw.db.privacy
  (:require [org.altlaw.db.data :as data]
            [clojure.set :as set]))


;;; Documents marked for no indexing by search engine robots

(defn get-norobots []
  (data/get-data "norobots"))

(defn create-norobots
  "Creates and initializes the norobots store."
  []
  (data/create-data "norobots" #{}))

(defn add-norobots
  "Adds a collection of document IDs to the norobots store."
  [docids]
  (dosync (commute (get-norobots) set/union (set docids))))

(defn save-norobots
  "Save the norobots store."
  []
  (data/save-data "norobots"))

(defn norobots?
  "Check if the given docid is in the norobots store."
  [docid]
  (contains? @(get-norobots) docid))


;;; Documents removed from the site entirely

(defn get-removed []
  (data/get-data "removed"))

(defn create-removed
  "Creates and initializes the removed store."
  []
  (data/create-data "removed" #{}))

(defn add-removed
  "Adds a collection of document IDs to the removed store."
  [docids]
  (dosync (commute (get-removed) set/union (set docids))))

(defn save-removed
  "Save the removed store."
  []
  (data/save-data "removed"))

(defn removed?
  "Check if the given docid is in the removed store."
  [docid]
  (contains? @(get-removed) docid))
