(ns org.altlaw.util.log
  (:require [org.altlaw.util.string :as str]
            [clojure.contrib.walk :as walk])
  (:import (org.apache.commons.logging Log LogFactory)
           (org.altlaw.util.logging
            JavaLoggingToCommonLoggingRedirector)))

(JavaLoggingToCommonLoggingRedirector/activate)

(defmacro trace [label arg]
  `(let [value# ~arg
         log# (. LogFactory getLog ~(str (ns-name *ns*)))]
     (when (. log# isTraceEnabled)
       (. log# (trace (str ~label ":" (pr-str value#)))))
     value#))

(defmacro debug [& args]
  `(let [log# (. LogFactory getLog ~(str (ns-name *ns*)))]
     (when (. log# isDebugEnabled)
       (. log# (debug (str ~@args))))))

(defmacro info [& args]
  `(let [log# (. LogFactory getLog ~(str (ns-name *ns*)))]
     (when (. log# isInfoEnabled)
       (. log# (info (str ~@args))))))

(defmacro warn [& args]
  `(let [log# (. LogFactory getLog ~(str (ns-name *ns*)))]
     (when (. log# isWarnEnabled)
       (. log# (warn (str ~@args))))))

(defmacro error [& args]
  `(let [log# (. LogFactory getLog ~(str (ns-name *ns*)))]
     (when (. log# isErrorEnabled)
       (. log# (error (str ~@args))))))

(defmacro fatal [& args]
  `(let [log# (. LogFactory getLog ~(str (ns-name *ns*)))]
     (when (. log# isFatalEnabled)
       (. log# (fatal (str ~@args))))))

(defn- truncate-strings
  "Find all strings in x and truncate them to max characters."
  [x max]
  (walk/prewalk (fn [y] (if (string? y) (str/truncate y max) y)) x))

(defn logstr
  "Print shortened form of arg suitable for logging."
  [arg]
  (binding [*print-length* 20
            *print-level* 10]
    (pr-str (truncate-strings arg 70))))
