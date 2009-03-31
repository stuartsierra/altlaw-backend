(ns org.altlaw.test.internal.idserver.client
  (use org.altlaw.internal.idserver.client
       clojure.contrib.test-is)
  (require org.altlaw.test.internal))

(use-fixtures :once 
              org.altlaw.test.internal/internal-server-fixture
              org.altlaw.test.internal/internal-uri-fixture)

(deftest can-set-and-get-new-values
  (let [coll (name (gensym "coll"))
        value (rand-int Integer/MAX_VALUE)]
    (set-value coll "one" value)
    (is (= value (get-value coll "one")))))

(deftest can-find-value
  (let [coll (name (gensym "coll"))]
    (find-value coll "foo")
    (is (integer? (get-value coll "foo")))))

(deftest find-value-returns-values-that-already-exist
  (let [coll (name (gensym "coll"))
        value (rand-int Integer/MAX_VALUE)]
    (set-value coll "foo" value)
    (is (= value (get-value coll "foo")))
    (is (= value (find-value coll "foo")))
    (is (= value (get-value coll "foo")))))

(deftest nonexistent-values-return-nil
  (let [coll (name (gensym "coll"))]
    (is (nil? (get-value coll "does-not-exist")))))

(deftest deleted-keys-return-nil
  (let [coll (name (gensym "coll"))
        value (rand-int Integer/MAX_VALUE)]
    (set-value coll "one" value)
    (is (= value (get-value coll "one")))
    (delete-key coll "one")
    (is (nil? (get-value coll "one")))))

(deftest can-get-entire-collection
  (let [coll (name (gensym "coll"))
        value1 (rand-int Integer/MAX_VALUE)
        value2 (rand-int Integer/MAX_VALUE)
        value3 (rand-int Integer/MAX_VALUE)
        correct {"one" value1, "two" value2,
                 "three" value3}]
    (set-value coll "one" value1)
    (set-value coll "two" value2)
    (set-value coll "three" value3)
    (is (= correct (into {} (get-map coll))))))
