(ns org.altlaw.util.restlet.error-log
  (:require [clojure.contrib.stacktrace :as stack])
  (:import  (org.altlaw.util DateUtils)
            (org.restlet.data Request)
            (org.apache.commons.logging Log LogFactory)))

(defn error-structure [exception status-code]
  (let [request (Request/getCurrent)
        client (.getClientInfo request)]
    {:exception (when exception (str exception))
     :stacktrace (when exception (with-out-str (stack/print-cause-trace exception)))
     :timestamp (DateUtils/timestamp)
     :request {:request_uri (str (.getOriginalRef request))
               :request_method (.getName (.getMethod request))
               :resource_uri (str (.getResourceRef request))
               :referrer_uri (str (.getReferrerRef request))
               :response_status_code status-code
               :client_address (.getAddress client)
               :user_agent (.getAgent client)}}))

(defn log-error-status [status]
  (let [exception (.getThrowable status)
        status-code (.getCode status)]
    (.error (LogFactory/getLog "org.altlaw.www.error")
            (pr-str (error-structure exception status-code)))))

