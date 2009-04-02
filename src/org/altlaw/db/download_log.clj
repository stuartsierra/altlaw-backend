(ns org.altlaw.db.download-log
  (:require [org.altlaw.util.simpledb :as db])
  (:import (java.util Date)
           (org.altlaw.util DateUtils)))

(def #^{:private true} *domain* "DownloadLog")

(defn download-date [url]
  (let [attrs (db/get-attrs *domain* url)]
    (when-let [date-string (get attrs "download_date")]
      (DateUtils/parseDateISO8601 date-string))))

(defn downloaded? [url]
  (if (download-date url) true false))

(defn enqueue-download [url]
  (db/put-attrs *domain* url
                {:queue_date (DateUtils/timestamp)}))

(defn record-download
  ([url]
     (db/put-attrs *domain* url
                   {:download_date (DateUtils/timestamp)}))
  ([url #^Date date]
     (db/put-attrs *domain* url
                   {:download_date
                    (.format DateUtils/ISO8601_GMT date)})))

