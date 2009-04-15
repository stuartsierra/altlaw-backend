(ns org.altlaw.www.insert-norobots
  (:require [org.altlaw.db.content :as content]))

(def *meta-tag* "<meta name=\"robots\" content=\"noindex\" />")

(defn insert-norobots-tag [docid]
  (println "Checking norobots on" docid)
  (try
   (let [path (str "v1/cases/" docid ".html")
         original (content/get-page path)]
     (if (.contains original *meta-tag*)
       (println "No changed needed for" docid)
       (let [modified (.replaceFirst original "</head>" (str *meta-tag* "</head>"))]
         (content/put-page-string path modified "text/html")
         (println "Changed " docid))))
   (catch Exception e
     (println "Exception" e "on" docid))))
