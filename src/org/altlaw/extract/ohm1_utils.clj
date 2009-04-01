(ns org.altlaw.extract.ohm1-utils
  (:require [org.altlaw.util.files :as files])
  (:refer clojure.set)
  (:import (java.io File)))

(def *court-dirs*
     {"ca1" "United States Court of Appeals for the First Circuit"
      "ca2" "United States Court of Appeals for the Second Circuit"
      "ca3" "United States Court of Appeals for the Third Circuit"
      "ca4" "United States Court of Appeals for the Fourth Circuit"
      "ca5" "United States Court of Appeals for the Fifth Circuit"
      "ca6" "United States Court of Appeals for the Sixth Circuit"
      "ca7" "United States Court of Appeals for the Seventh Circuit"
      "ca8" "United States Court of Appeals for the Eighth Circuit"
      "ca9" "United States Court of Appeals for the Ninth Circuit"
      "ca10" "United States Court of Appeals for the Tenth Circuit"
      "ca11" "United States Court of Appeals for the Eleventh Circuit"
      "cadc" "United States Court of Appeals for the District of Columbia Circuit"
      "cafc" "United States Court of Appeals for the Federal Circuit"
      "cafc-gtown" "United States Court of Appeals for the Federal Circuit"
      "sct" "United States Supreme Court"})

(def *court-dirs-uris*
     {"ca1" "http://id.altlaw.org/courts/us/fed/app/1"
      "ca2" "http://id.altlaw.org/courts/us/fed/app/2"
      "ca3" "http://id.altlaw.org/courts/us/fed/app/3"
      "ca4" "http://id.altlaw.org/courts/us/fed/app/4"
      "ca5" "http://id.altlaw.org/courts/us/fed/app/5"
      "ca6" "http://id.altlaw.org/courts/us/fed/app/6"
      "ca7" "http://id.altlaw.org/courts/us/fed/app/7"
      "ca8" "http://id.altlaw.org/courts/us/fed/app/8"
      "ca9" "http://id.altlaw.org/courts/us/fed/app/9"
      "ca10" "http://id.altlaw.org/courts/us/fed/app/10"
      "ca11" "http://id.altlaw.org/courts/us/fed/app/11"
      "cadc" "http://id.altlaw.org/courts/us/fed/app/12"
      "cafc" "http://id.altlaw.org/courts/us/fed/app/fed"
      "cafc-gtown" "http://id.altlaw.org/courts/us/fed/app/fed"
      "sct" "http://id.altlaw.org/courts/us/fed/supreme"})

(def *court-dir-regex* #"\b(?:ca\d+|cafc-gtown|cafc|cadc|sct)\b")

(defn ohm1-filename-key [path]
  (let [court-dir (re-find *court-dir-regex* path)
        filename (.replace (.getName (File. path)) "-01A" "")]
    (str court-dir "|" (re-find #"^[^.]+" filename))))

(defn ohm1-court-name [path]
  (*court-dirs* (re-find *court-dir-regex* path)))

(defn ohm1-court-uri [path]
  (*court-dirs-uris* (re-find *court-dir-regex* path)))

(defn ohm1-guess-mime-type [filename]
  (if-let [first-guess (files/guess-mime-type-by-name filename)]
      first-guess
    (cond
     ;; 1st cir. doesn't put extensions on its WPD and HTML files
     (.contains filename "ca1/wp/") "application/vnd.wordperfect"
     (re-find #"ca1/[^/]+\.01A$" filename) "text/html"
     :else 0)))
