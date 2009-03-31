(ns org.altlaw.test.internal.privacy.client
  (:use org.altlaw.internal.privacy.client
        org.altlaw.test.internal
        clojure.contrib.test-is))

(use-fixtures :once 
              org.altlaw.test.internal/internal-server-fixture
              org.altlaw.test.internal/internal-uri-fixture)

(deftest can-manage-norobots
  (let [ids (take 3 (repeatedly #(rand-int 1000)))]
    (add-norobots ids)
    (doseq [id ids]
      (is (some #{id} (get-norobots))))))

(deftest can-manage-removed
  (let [ids (take 3 (repeatedly #(rand-int 1000)))]
    (add-removed ids)
    (doseq [id ids]
      (is (some #{id} (get-removed))))))
