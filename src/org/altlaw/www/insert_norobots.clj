(ns org.altlaw.www.insert-norobots
  (:require [org.altlaw.db.content :as content]))

(def *meta-tag* "<meta name=\"robots\" content=\"noindex\" />")

(defn insert-norobots-tag [docid]
  (let [path (str "v1/cases/" docid ".html")
        original (content/get-page path)]
    (if (.contains original *meta-tag*)
      :no-change-needed
      (let [modified (.replaceFirst original "</head>" (str *meta-tag* "</head>"))]
        (content/put-page-string path modified "text/html")
        :changed))))
