(ns org.altlaw.extract.pdf
  (:require [clojure.contrib.singleton :as sing]
            [org.altlaw.util.log :as log])
  (:import (org.altlaw.util RunProgramOnFile)
           (org.apache.commons.exec ExecuteException)
           (java.io File)))

(def *pdf-to-html-executable* (File. "/usr/local/bin/altlaw_parse_pdf"))

(def #^{:private true} get-runner
     (sing/per-thread-singleton
      (fn []
        (doto (RunProgramOnFile.)
          (.setExecutable *pdf-to-html-executable*)))))


(defn pdf-to-html [filename bytes]
  (try 
   (String. (.exec (get-runner) bytes) "UTF-8")
   (catch ExecuteException e
     (log/warn "altlaw_parse_pdf failed on " filename ": " e)
     nil)))
