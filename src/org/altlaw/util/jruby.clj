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

