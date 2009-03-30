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
  (extract-content-with #(.getProperty props %)))

(defmethod extract-context org.restlet.Context [context]
  (let [attrs (.getAttributes context)]
    (extract-content-with #(.get attrs %))))

(defmethod extract-context org.apache.hadoop.conf.Configuration [config]
  (extract-content-with #(.get config %)))


;;; PUBLIC

(defn setup-context [obj]
  (alter-var-root #'*altlaw-context*
                  (fn [old] (merge *default-context* (extract-context obj)))))

(defn altlaw-home []
  (:home *altlaw-context*))

(defn altlaw-env []
  (:env *altlaw-context*))

(defn altlaw-internal-uri []
  (:internal-uri *altlaw-context*))

(defmacro with-altlaw-env [name & body] 
  `(binding [*altlaw-context* (assoc *altlaw-context* :env name)]
     ~@body))

(defn testing-env-fixture [f]
  (with-altlaw-env "testing" (f)))
