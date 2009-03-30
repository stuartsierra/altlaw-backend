(ns org.altlaw.internal.idserver.impl
  (:use [clojure.contrib.sql :as sql]
        [clojure.contrib.duck-streams :as duck]
        [clojure.contrib.test-is :only (with-test is)]
        [org.altlaw.constants :only (*internal-db* with-environment *altlaw-env*)]))


(defmacro #^{:private true} with-testing-vars [& body]
  `(with-environment "testing"
     (sql/with-connection
      *internal-db*
      (try (sql/drop-table "coll_docids") (catch Exception e# nil)))
     (setup-tables)
     (sql/with-connection
      *internal-db*
      (sql/insert-rows "coll_docids"
                       ["testing" "one" 1]
                       ["testing" "two" 2]))
     ~@body))

(defn setup-tables []
  (sql/with-connection
   *internal-db*
   (try (sql/do-commands "CREATE TABLE coll_docids (
coll_name CHAR(20) NOT NULL,
doc_key VARCHAR(255) NOT NULL,
docid INT,
PRIMARY KEY (coll_name,doc_key))")
        (.. java.util.logging.Logger (getLogger "org.altlaw")
            (info (str "Created coll_docids table for "
                       *altlaw-env* " environment.")))
        (catch Exception e
          (if (.contains (.getMessage e) "already exists")
            (do (.. java.util.logging.Logger (getLogger "org.altlaw")
                    (info (str "coll_docids table already exists in "
                               *altlaw-env* " environment.")))) 
            (throw e))))))

(with-test
    (defn get-value [map-name key]
      (let [stmt "SELECT docid FROM coll_docids 
WHERE coll_name = ? AND doc_key = ?"]
        (sql/with-connection
         *internal-db* (sql/with-query-results
               result [stmt map-name key]
               (:docid (first result))))))
  (with-testing-vars
   (is (= 1 (get-value "testing" "one")))
   (is (= 2 (get-value "testing" "two")))
   (is (nil? (get-value "testing" "does not exist")))))

(with-test
    (defn get-map [map-name]
      (let [stmt "SELECT doc_key,docid FROM coll_docids WHERE coll_name = ?"]
        (sql/with-connection
         *internal-db* (sql/with-query-results
               result [stmt map-name]
               (when-not (nil? result)
                 (doall (map (fn [entry] [(:doc_key entry) (:docid entry)])
                             result)))))))
  (with-testing-vars
    (is (= {"one" 1, "two" 2} (into {} (get-map "testing"))))
    (is (nil? (get-map "does not exist")))))

(defn get-next-docid []
  (sql/with-connection
   *internal-db* (sql/with-query-results
         result ["SELECT MAX(docid) FROM coll_docids"]
         (inc (or (:1 (first result)) 10000)))))

(with-test
    (defn set-value [map-name key value]
      (.. java.util.logging.Logger (getLogger "org.altlaw")
          (info (str "Called idserver set-value in "
                     *altlaw-env* " environment.")))
      (sql/with-connection
       *internal-db*
       (sql/update-or-insert-values "coll_docids" ["coll_name = ? AND doc_key = ?" map-name key]
                                    {:coll_name map-name :doc_key key :docid value}))
      value)
  (with-testing-vars
   (let [n (rand-int Integer/MAX_VALUE)]
     (is (nil? (get-value "testing" "random")))
     (set-value "testing" "random" n)
     (is (= n (get-value "testing" "random")))
     (set-value "testing new" "random" n)
     (is (= n (get-value "testing new" "random"))))))

(with-test
    (defn find-value [map-name key]
      (sql/with-connection
       *internal-db* (sql/transaction
                      (or (get-value map-name key)
                          (set-value map-name key (get-next-docid))))))
(with-testing-vars
  (is (= 3 (find-value "testing" "three")))
  (is (= 4 (find-value "testing" "four")))

  (set-value "testing" "foobar" 101)
  (is (= 101 (find-value "testing" "foobar")))
  (is (= 101 (get-value "testing" "foobar")))))

(with-test
    (defn delete-key [map-name key]
      (sql/with-connection
       *internal-db* (sql/delete-rows
             "coll_docids"
             ["coll_name = ? AND doc_key = ?" map-name key])))
  (with-testing-vars
    (is (= 1 (get-value "testing" "one")))
    (delete-key "testing" "one")
    (is (nil? (get-value "testing" "one")))))

(with-test
    (defn load-keymap [map-name input]
      (let [pairs (map (fn [line]
                         (apply vector map-name (seq (.split line "\t"))))
                       (duck/read-lines input))]
        (sql/with-connection
         *internal-db* (sql/transaction
               (sql/delete-rows "coll_docids" ["coll_name = ?" map-name])
               (apply sql/insert-rows "coll_docids" pairs)))))
  (with-testing-vars
    (load-keymap "testing"
                 (java.io.StringReader. "alpha\t50\nbeta\t51"))
    (is (= 50 (get-value "testing" "alpha")))
    (is (= 51 (get-value "testing" "beta")))
    (is (nil? (get-value "testing" "one")))))

