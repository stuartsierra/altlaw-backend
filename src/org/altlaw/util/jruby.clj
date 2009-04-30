(ns org.altlaw.util.jruby
  (:require [clojure.contrib.singleton :as sing]
            [clojure.contrib.java-utils :as j])
  (:import (javax.script SimpleBindings ScriptContext
                         ScriptEngine ScriptEngineManager)))

(def #^{:private true} jruby
     (sing/per-thread-singleton
      (fn []
        (.getEngineByName (ScriptEngineManager.) "jruby"))))

(defn- make-bindings [m]
  (let [bindings (SimpleBindings.)]
   (doseq [[k v] m]
     (.put bindings (j/as-str k) v))
   bindings))

(defn eval-jruby
  ([script]
     (.eval (jruby) script))
  ([script bindings]
     (.eval (jruby) script (make-bindings bindings))))


(derive java.lang.String ::java-object)
(derive java.lang.Number ::java-object)
(derive java.lang.Boolean ::java-object)

(defmulti convert-jruby class)

(defmethod convert-jruby org.jruby.RubySymbol [x]
  (keyword (str x)))

(defmethod convert-jruby org.jruby.RubyObject [x]
  ;; converting a Date gives "YYYY-MM-DD"
  (str x))

(defmethod convert-jruby org.jruby.RubyHash [x]
  (reduce (fn [m [k v]]
            (assoc m (convert-jruby k)
                   (convert-jruby v)))
          {} x))

(defmethod convert-jruby org.jruby.RubyArray [x]
  (vec (map convert-jruby x)))

(defmethod convert-jruby ::java-object [x] x)

(defmethod convert-jruby nil [x] x)