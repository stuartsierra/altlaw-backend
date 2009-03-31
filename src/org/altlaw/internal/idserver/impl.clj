(ns org.altlaw.internal.idserver.impl
  (:use org.altlaw.util.log)
  (:require [org.altlaw.util.context :as context]
            [clojure.contrib.sql :as sql]
            [clojure.contrib.duck-streams :as duck]))


(defn setup-tables []
  (sql/with-connection
   (context/internal-db)
   (try (sql/do-commands "CREATE TABLE coll_docids (
coll_name CHAR(20) NOT NULL,
doc_key VARCHAR(255) NOT NULL,
docid INT,
PRIMARY KEY (coll_name,doc_key))")
        (info "Created coll_docids table for "
              (context/altlaw-env) " environment.")
        (catch Exception e
          (if (.contains (.getMessage e) "already exists")
            (info (str "coll_docids table already exists in "
                       (context/altlaw-env) " environment.")) 
            (throw e))))))

(defn get-value [map-name key]
  (let [stmt "SELECT docid FROM coll_docids 
WHERE coll_name = ? AND doc_key = ?"]
    (sql/with-connection
     (context/internal-db) (sql/with-query-results
                            result [stmt map-name key]
                            (:docid (first result))))))

(defn get-map [map-name]
  (let [stmt "SELECT doc_key,docid FROM coll_docids WHERE coll_name = ?"]
    (sql/with-connection
     (context/internal-db) (sql/with-query-results
                            result [stmt map-name]
                            (when-not (or (nil? result) (empty? result))
                              (doall (map (fn [entry] [(:doc_key entry) (:docid entry)])
                                          result)))))))

(defn get-next-docid []
  (sql/with-connection
   (context/internal-db) (sql/with-query-results
         result ["SELECT MAX(docid) FROM coll_docids"]
         (inc (or (:1 (first result)) 10000)))))

(defn set-value [map-name key value]
  (info (str "Called idserver set-value in "
             (context/altlaw-env) " environment."))
  (sql/with-connection
   (context/internal-db)
   (sql/update-or-insert-values "coll_docids" ["coll_name = ? AND doc_key = ?" map-name key]
                                {:coll_name map-name :doc_key key :docid value}))
  value)

(defn find-value [map-name key]
  (sql/with-connection
   (context/internal-db) (sql/transaction
                          (or (get-value map-name key)
                              (set-value map-name key (get-next-docid))))))

(defn delete-key [map-name key]
  (sql/with-connection
   (context/internal-db) (sql/delete-rows
                          "coll_docids"
                          ["coll_name = ? AND doc_key = ?" map-name key])))

(defn load-keymap [map-name input]
  (let [pairs (map (fn [line]
                     (apply vector map-name (seq (.split line "\t"))))
                   (duck/read-lines input))]
    (sql/with-connection
     (context/internal-db) (sql/transaction
                            (sql/delete-rows "coll_docids" ["coll_name = ?" map-name])
                            (apply sql/insert-rows "coll_docids" pairs)))))

