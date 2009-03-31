(ns org.altlaw.test.run
  (:require [clojure.contrib.test-is :as t])
  (:gen-class))

(defn -main [& args]
  (require 'org.altlaw.load-all)
  (if (or (nil? args) (empty? args))
    (apply t/run-tests 
           (filter #(.startsWith (str (ns-name %)) "org.altlaw.")
                   (all-ns)))
    (apply t/run-tests (map #(find-ns (symbol %)) args))))

