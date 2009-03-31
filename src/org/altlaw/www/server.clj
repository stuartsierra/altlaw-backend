(ns org.altlaw.www.server
  (:gen-class)
  (:require [org.altlaw.util.context :as context]
            [org.altlaw.util.restlet :as component]
            org.altlaw.www.application
            org.altlaw.util.restlet.status_service_dev
            org.altlaw.www.status_service_public))

(defn setup-logging [component]
  (let [service (.getLogService component)]
    (.setLoggerName service "org.altlaw.www.access")))

(defn -main [& args]
  (let [app (new org.altlaw.www.application)
        port (if (= (context/altlaw-env) "production") 80 8080)]
    (if (= (context/altlaw-env) "production")
      (.setStatusService app (new org.altlaw.www.status_service_public))
      (.setStatusService app (new org.altlaw.util.restlet.status_service_dev)))
    (println "Starting www server at port" port "in"
             (context/altlaw-env) "environment.")
    (let [component (component/make-component port app)]
      (setup-logging component)
      (.start component))))
