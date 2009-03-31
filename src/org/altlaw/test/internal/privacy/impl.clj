(ns org.altlaw.test.internal.privacy.impl
  (:use org.altlaw.internal.privacy.impl
        clojure.contrib.test-is)
  (:require [org.altlaw.util.context :as context]
            [clojure.contrib.sql :as sql]
            [clojure.contrib.duck-streams :as duck]))

(defn table-fixture [f]
  (sql/with-connection
   (context/internal-db)
   (try (sql/drop-table "privacy_complaints")
        (sql/drop-table "privacy_docs")
        (sql/drop-table "privacy_actions")
        (catch Exception e# nil)))
  (setup-tables)
  (sql/with-connection
   (context/internal-db)
   (sql/do-commands
    "INSERT INTO privacy_actions 
 (action_datetime, docid, action)
VALUES ('2007-01-05 12:42:00', 999, 1),
 ('2007-01-05 12:42:00', 998, 2),
 ('2007-01-05 12:42:00', 997, 1),
 ('2007-01-05 12:42:00', 996, 1)"))
  (f))

(use-fixtures :each table-fixture)

(deftest t-get-removed
 (is (= #{998} (get-removed))))

(deftest t-get-norobots
 (is (= #{996 997 999} (get-norobots))))

(deftest t-add-norobots
 (add-norobots #{98 99})
 (is (contains? (get-norobots) 98))
 (is (contains? (get-norobots) 99)))

(deftest t-add-removed
 (add-removed #{96 96})
 (is (contains? (get-removed) 96))
 (is (contains? (get-removed) 96)))


(deftest t-add-complaint
  (add-complaint "Complaint one" [101 102])
  (sql/with-connection
   (context/internal-db)
   (sql/with-query-results
    results ["SELECT * FROM privacy_complaints"]
    (is (some #(= (duck/slurp* (.getCharacterStream (:complaint_text %)))
                  "Complaint one")
              results)))
   (sql/with-query-results
    results ["SELECT docid FROM privacy_docs D, privacy_complaints C
WHERE C.complaint_datetime = D.complaint_datetime
AND CAST(c.complaint_text AS VARCHAR(255)) = 'Complaint one'"]
    (is (= #{101 102} (set (map :docid results)))))))
