(ns org.altlaw.test.run
  (:require [clojure.contrib.test-is :as t])
  (:gen-class))

(defn -main [& args]
  (require 'org.altlaw.load-all)
  (t/run-all-tests))

