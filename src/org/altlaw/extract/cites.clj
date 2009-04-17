(ns org.altlaw.extract.cites
  (:import (java.net URLEncoder)))

(def *citation-regex* #"\d+ (?:F\.2d|F\.3d|U\.S\.) \d+")

(defn find-citations [text]
  (re-seq *citation-regex* text))

(defn link-citations [html]
  (let [buffer (StringBuffer.)
        matcher (re-matcher *citation-regex* html)]
    (loop []
      (if (.find matcher)
        (let [cite (.group matcher)
              link (str "<a href=\"/cite/" (URLEncoder/encode cite)
                        "\">" cite "</a>")]
          (.appendReplacement matcher buffer link)
          (recur))
        (do (.appendTail matcher buffer)
            (str buffer))))))
