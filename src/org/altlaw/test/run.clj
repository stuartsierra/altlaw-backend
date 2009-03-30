(ns org.altlaw.test.run
  (:require org.altlaw.load-all
            [clojure.contrib.test-is :as t])
  (:gen-class))

(defn -main [& args]
  (t/run-all-tests))

