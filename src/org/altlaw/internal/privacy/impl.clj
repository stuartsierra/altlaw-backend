(ns org.altlaw.internal.privacy.impl
  (:use org.altlaw.util.log)
  (:require [org.altlaw.util.context :as context]
            clojure.set
            [clojure.contrib.sql :as sql]
            [clojure.contrib.duck-streams :as duck])
  (:import (java.sql Timestamp)
           (java.util Date)))


(def *numbers-to-actions*
     {1 "norobots"
      2 "removed"})

(def *actions-to-numbers*
     (clojure.set/map-invert *numbers-to-actions*))

(defn setup-tables []
  (sql/with-connection
   (context/internal-db)
   (try (sql/do-commands 
"CREATE TABLE privacy_complaints (
complaint_datetime TIMESTAMP,
complaint_text CLOB,
PRIMARY KEY (complaint_datetime))"

"CREATE TABLE privacy_docs (
complaint_datetime TIMESTAMP,
docid INTEGER,
PRIMARY KEY (complaint_datetime, docid))"

"CREATE TABLE privacy_actions (
action_datetime TIMESTAMP,
docid INTEGER,
action INTEGER,
PRIMARY KEY (action_datetime, docid))")
        (info "Created privacy tables for "
              (context/altlaw-env) " environment.")
        (catch Exception e
          (if (.contains (.getMessage e) "already exists")
            (info "Privacy tables already exist in "
                  (context/altlaw-env) " environment.")
            (throw e))))))

(defn- get-docid-list [action-name]
  (sql/with-connection
    (context/internal-db)
    (sql/with-query-results
     results ["SELECT docid FROM privacy_actions WHERE action = ?"
              (*actions-to-numbers* action-name)]
     (set (map :docid results)))))

(defn get-removed []
  (get-docid-list "removed"))

(defn get-norobots []
  (get-docid-list "norobots"))

(defn- add-docid-list [action-name docids]
  (let [timestamp (Timestamp. (.getTime (Date.)))
        action (*actions-to-numbers* action-name)]
    (sql/with-connection
     (context/internal-db)
     (apply sql/insert-values
            "privacy_actions" ["action_datetime" "docid" "action"]
            (map #(vector timestamp % action) docids)))))

(defn add-norobots [docids]
  (add-docid-list "norobots" docids))

(defn add-removed [docids]
  (add-docid-list "removed" docids))


(defn add-complaint
  ([text docids]
     (add-complaint (Timestamp. (.getTime (Date.))) text docids))
  ([timestamp text docids]
     (sql/with-connection
      (context/internal-db)
      (sql/transaction
       ;; insert into privacy_complaints
       (sql/insert-values
        "privacy_complaints"
        ["complaint_datetime" "complaint_text"]
        [timestamp text])
       ;; insert into privacy_docs
       (apply sql/insert-values "privacy_docs"
              ["complaint_datetime" "docid"]
              (map #(vector timestamp %) docids))))))
