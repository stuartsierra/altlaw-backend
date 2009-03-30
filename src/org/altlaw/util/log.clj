(ns org.altlaw.util.log
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

