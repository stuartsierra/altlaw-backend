(ns org.altlaw.util.context)

(defn get-property [name]
  (or (System/getProperty name)
      (throw (Exception. (str "Missing property " name)))))


;;; PUBLIC

(defn altlaw-home
  "AltLaw's home directory, as String."
  []
  (get-property "org.altlaw.home"))

(defn altlaw-env
  "AltLaw's running environment, a String.
  One of production/development/testing."
  []
  (get-property "org.altlaw.env"))

(defn internal-uri
  "The base URI of the internal.altlaw.org server.  String, includes
  the scheme and host name, but not the initial slash."
  []
  (get-property "org.altlaw.internal.uri"))

(defn internal-db
  "The DB description for clojure.contrib.sql/with-connection"
  []
  {:classname "org.apache.derby.jdbc.EmbeddedDriver"
   :subprotocol "derby"
   :subname (java.io.File. (str (altlaw-home) "/var/db/" (altlaw-env)))
   :create true})

