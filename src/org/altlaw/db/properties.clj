(ns org.altlaw.db.properties
  (:require [org.altlaw.util.simpledb :as db]
            [clojure.contrib.java-utils :as j]))

(def *domain* "properties")

(defn create-properties-domain []
  (.createDomain (db/get-simpledb) "properties"))

(defn set-property [p-name value]
  (db/put-attrs *domain* (j/as-str p-name) {:value value}))

(defn get-property [p-name]
  (get (db/get-attrs *domain* (j/as-str p-name)) "value"))

(defn delete-property [p-name]
  (db/delete-item *domain* (j/as-str p-name)))
