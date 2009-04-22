(ns org.altlaw.db.download-log
  (:require [org.altlaw.db.data :as data])
  (:import (java.util Date Calendar)
           (org.altlaw.util DateUtils)))

(defn get-download-log []
  (data/get-data "download-log"))

(defn create-download-log []
  (data/create-data "download-log" {}))

(defn save-download-log []
  (data/save-data "download-log"))

(defn- check-date-format
  "If date is a Date or Calendar object, convert to an ISO-8601
  string.  If it is a string, check that it is in ISO-8601 format.
  Returns an ISO-8601 string or throws exception on bad input."
  [date]
  (cond
   (string? date) (DateUtils/dateToISO8601
                   (DateUtils/parseDateISO8601 date))
   (instance? Date date) (DateUtils/dateToISO8601 date)
   (instance? Calendar date) (DateUtils/calendarToISO8601 date)
   :else (throw (Exception. "date must be an ISO-8601 String, a Date, or a Calendar"))))

(defn log-download
  "Logs a download of the URL (a String). If date (Date object or
  ISO-8601 date/time string) is not given, assume current date/time."
  ([url]
     (assert (string? url))
     (dosync (alter (get-download-log)
                    assoc url (DateUtils/timestamp))))
  ([url date]
     (assert (string? url))
     (dosync (alter (get-download-log)
                    assoc url (check-date-format date)))))

(defn downloaded?
  "Returns true if the URL (a String) has already been downloaded."
  [url]
  (assert (string? url))
  (contains? @(get-download-log) url))
