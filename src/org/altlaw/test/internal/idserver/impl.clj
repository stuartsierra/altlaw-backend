(ns org.altlaw.test.internal.idserver.impl
  (:use org.altlaw.internal.idserver.impl
        clojure.contrib.test-is)
  (:require [org.altlaw.util.context :as context]
            [clojure.contrib.sql :as sql]))

(defn table-fixture [f]
  (sql/with-connection
   (context/internal-db)
   (try (sql/drop-table "coll_docids") (catch Exception e# nil)))
  (setup-tables)
  (sql/with-connection
   (context/internal-db)
   (sql/insert-rows "coll_docids"
                    ["testing" "one" 1]
                    ["testing" "two" 2]))
  (f))

(use-fixtures :each table-fixture)

(deftest t-get-value
  (is (= 1 (get-value "testing" "one")))
  (is (= 2 (get-value "testing" "two")))
  (is (nil? (get-value "testing" "does not exist"))))

(deftest t-get-map
  (is (= {"one" 1, "two" 2} (into {} (get-map "testing"))))
  (is (nil? (get-map "does not exist"))))

(deftest t-set-value
  (let [n (rand-int Integer/MAX_VALUE)]
    (is (nil? (get-value "testing" "random")))
    (set-value "testing" "random" n)
    (is (= n (get-value "testing" "random")))
    (set-value "testing new" "random" n)
    (is (= n (get-value "testing new" "random")))))

(deftest t-find-value
  (is (= 3 (find-value "testing" "three")))
  (is (= 4 (find-value "testing" "four")))

  (set-value "testing" "foobar" 101)
  (is (= 101 (find-value "testing" "foobar")))
  (is (= 101 (get-value "testing" "foobar"))))

(deftest t-delete-key 
  (is (= 1 (get-value "testing" "one")))
  (delete-key "testing" "one")
  (is (nil? (get-value "testing" "one"))))

(deftest t-load-keymap
  (load-keymap "testing"
               (java.io.StringReader. "alpha\t50\nbeta\t51"))
  (is (= 50 (get-value "testing" "alpha")))
  (is (= 51 (get-value "testing" "beta")))
  (is (nil? (get-value "testing" "one"))))
