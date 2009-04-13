(ns org.altlaw.util.context)

(defn get-property-function [name]
  (System/getProperty name))

(defn get-property [name]
  (or (get-property-function name)
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

(defn www-content-dir
  "Root directory for markdown content files for 'about' pages on
  www.altlaw.org" []
  (java.io.File. (str (altlaw-home) "/src/org/altlaw/www/content")))

(defn www-public-dir
  "Root directory for public files generated by code and served via
  HTTP on www.altlaw.org" []
  (java.io.File. (str (altlaw-home) "/var/public")))

(defn www-static-dir 
  "Root directory for public files under version control that are
  served via HTTP on www.altlaw.org" []
  (java.io.File. (str (altlaw-home) "/src/org/altlaw/www/static")))

(defn solr-home 
  "Solr home directory for running Solr instances."
  []
  (java.io.File. (str (altlaw-home) "/var/solr")))

(defn aws-access-key-id []
  (get-property "org.altlaw.aws.access.key.id"))

(defn aws-secret-access-key []
  (get-property "org.altlaw.aws.secret.access.key"))
