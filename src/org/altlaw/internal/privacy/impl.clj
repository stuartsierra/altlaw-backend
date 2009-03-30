(ns org.altlaw.internal.privacy.impl
  (:use [clojure.contrib.sql :as sql]
        [clojure.contrib.test-is :only (with-test is)]
        [clojure.contrib.duck-streams :as duck]
        [org.altlaw.constants :only (*internal-db* with-environment *altlaw-env*)])
  (:import (java.sql Timestamp)
           (java.util Date)))


(defmacro #^{:private true} with-testing-vars [& body]
  `(with-environment "testing"
     (sql/with-connection
      *internal-db*
      (try (sql/drop-table "privacy_complaints")
           (sql/drop-table "privacy_docs")
           (sql/drop-table "privacy_actions")
           (catch Exception e# nil)))
     (setup-tables)
     (sql/with-connection
      *internal-db*
      (sql/do-commands
       "INSERT INTO privacy_actions 
 (action_datetime, docid, action)
VALUES ('2007-01-05 12:42:00', 999, 1),
 ('2007-01-05 12:42:00', 998, 2),
 ('2007-01-05 12:42:00', 997, 1),
 ('2007-01-05 12:42:00', 996, 1)"))
     ~@body))

(def *numbers-to-actions*
     {1 "norobots"
      2 "removed"})

(def *actions-to-numbers*
     (clojure.set/map-invert *numbers-to-actions*))

(defn setup-tables []
  (sql/with-connection
   *internal-db*
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
        (.. java.util.logging.Logger (getLogger "org.altlaw")
            (info (str "Created privacy tables for "
                       *altlaw-env* " environment.")))
        (catch Exception e
          (if (.contains (.getMessage e) "already exists")
            (.. java.util.logging.Logger (getLogger "org.altlaw")
                (info (str "Privacy tables already exist in "
                           *altlaw-env* " environment.")))
            (throw e))))))

(defn- get-docid-list [action-name]
  (sql/with-connection
    *internal-db*
    (sql/with-query-results
     results ["SELECT docid FROM privacy_actions WHERE action = ?"
              (*actions-to-numbers* action-name)]
     (set (map :docid results)))))

(with-test
 (defn get-removed []
   (get-docid-list "removed"))
 (with-testing-vars
  (is (= #{998} (get-removed)))))

(with-test
 (defn get-norobots []
   (get-docid-list "norobots"))
 (with-testing-vars
  (is (= #{996 997 999} (get-norobots)))))

(defn- add-docid-list [action-name docids]
  (let [timestamp (Timestamp. (.getTime (Date.)))
        action (*actions-to-numbers* action-name)]
    (sql/with-connection
     *internal-db*
     (apply sql/insert-values
            "privacy_actions" ["action_datetime" "docid" "action"]
            (map #(vector timestamp % action) docids)))))

(with-test
 (defn add-norobots [docids]
   (add-docid-list "norobots" docids))
 ;; test:
 (with-testing-vars
   (add-norobots #{98 99})
   (is (contains? (get-norobots) 98))
   (is (contains? (get-norobots) 99))
   (sql/with-connection
    *internal-db*
    (sql/delete-rows "privacy_actions" ["docid = 98 OR docid = 99"]))))

(with-test
 (defn add-removed [docids]
   (add-docid-list "removed" docids))
 ;; test:
 (with-testing-vars
   (add-removed #{96 96})
   (is (contains? (get-removed) 96))
   (is (contains? (get-removed) 96))
   (sql/with-connection
    *internal-db*
    (sql/delete-rows "privacy_actions" ["docid = 96 OR docid = 97"]))))


(with-test
    (defn add-complaint
      ([text docids]
         (add-complaint (Timestamp. (.getTime (Date.))) text docids))
      ([timestamp text docids]
         (sql/with-connection
          *internal-db*
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
  ;; tests:
  (with-testing-vars
    (add-complaint "Complaint one" [101 102])
    (sql/with-connection
     *internal-db*
     (sql/with-query-results
      results ["SELECT * FROM privacy_complaints"]
      (is (some #(= (duck/slurp* (.getCharacterStream (:complaint_text %)))
                    "Complaint one")
                results)))
     (sql/with-query-results
      results ["SELECT docid FROM privacy_docs D, privacy_complaints C
WHERE C.complaint_datetime = D.complaint_datetime
AND CAST(c.complaint_text AS VARCHAR(255)) = 'Complaint one'"]
      (is (= #{101 102} (set (map :docid results))))))))
