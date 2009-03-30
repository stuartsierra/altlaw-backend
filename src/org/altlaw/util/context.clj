(ns org.altlaw.util.context)

(def #^{:private true} *altlaw-context*)

(def #^{:private true}
     *default-context*
     {:home (System/getProperty "user.dir")
      :env "development"})

;; f is fn encapsulating a method call to some context object, that
;; should return values for string keys.
(defn- extract-context-with [f]
  {:home (f "org.altlaw.home")
   :env (f "org.altlaw.env")
   :internal-uri (f "org.altlaw.internal.uri")})

;; Extract context information from the given object.  May be a
;; java.util.Properties, org.restlet.Context, or
;; org.apache.hadoop.conf.Configuration.
(defmulti #^{:private true} extract-context class)

(defmethod extract-context java.util.Properties [props]
  (extract-context-with #(.getProperty props %)))

(defmethod extract-context org.restlet.Context [context]
  (let [attrs (.getAttributes context)]
    (extract-context-with #(.get attrs %))))

(defmethod extract-context org.apache.hadoop.conf.Configuration [config]
  (extract-context-with #(.get config %)))


;;; PUBLIC

(defn setup-context
  "Set up AltLaw's context object from the given context/configuration
  object."
  [obj]
  (alter-var-root #'*altlaw-context*
                  (fn [old] (merge *default-context* (extract-context obj)))))

(defn altlaw-home
  "AltLaw's home directory, as String."
  []
  (:home *altlaw-context*))

(defn altlaw-env
  "AltLaw's running environment, a String.
  One of production/development/testing."
  []
  (:env *altlaw-context*))

(defn internal-uri
  "The base URI of the internal.altlaw.org server.  String, includes
  the scheme and host name, but not the initial slash."
  []
  (:internal-uri *altlaw-context*))

(defn internal-db
  "The DB description for clojure.contrib.sql/with-connection"
  []
  {:classname "org.apache.derby.jdbc.EmbeddedDriver"
   :subprotocol "derby"
   :subname (str (altlaw-home) "/var/db/" (altlaw-env))
   :create true})

(defmacro with-altlaw-env
  "Run body with a modified altlaw-env string."
  [name & body] 
  `(binding [*altlaw-context* (assoc *altlaw-context* :env name)]
     ~@body))

(defn testing-env-fixture
  "Fixture function for use in tests.  Sets the altlaw-env to
  'testing'."
  [f]
  (with-altlaw-env "testing" (f)))
