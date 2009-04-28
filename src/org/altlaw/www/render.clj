(ns org.altlaw.www.render
  (:require [org.altlaw.util.context :as context]
            [clojure.contrib.java-utils :as java]
            [clojure.contrib.classpath :as cp]
            [clojure.contrib.singleton :as sing])
  (:use [clojure.contrib.test-is :only (with-test is testing)]
        [clojure.contrib.walk :only (stringify-keys)])
  (:import (org.antlr.stringtemplate StringTemplate StringTemplateGroup)
           (org.antlr.stringtemplate.language DefaultTemplateLexer)
           (org.apache.commons.lang StringEscapeUtils)))

(with-test
 (defn- render-template
   "Assigns attributes to a StringTemplate and renders it.  Attributes
   are a :keyword=>value map, where values may be scalars or collections."
   [template attrs]
   (.setAttributes template (stringify-keys attrs))
   (str template))
 
 (is (= "Goodbye, Bob!"
        (render-template (StringTemplate. "$greeting$, $name$!")
                         {:greeting "Goodbye" :name "Bob"})))

 (testing "list separators"
          (is (= "Welcome, foo,bar,baz."
                 (render-template (StringTemplate.
                                   "$greeting$, $names; separator=\",\"$.")
                                  {:greeting "Welcome"
                                   :names ["foo" "bar" "baz"]}))))
 (testing "rest() operator"
          (is (= "Hello: First:foo,bar,baz"
                 (render-template (StringTemplate.
                                   "Hello: First:$first(names)$,$rest(names); separator=\",\"$")
                                  {:names ["foo" "bar" "baz"]}))))

 (testing "last() operator"
          (is (= "foo,bar,baz,last:baz"
                 (render-template (StringTemplate.
                                   "$names; separator=\",\"$,last:$last(names)$")
                                  {:names ["foo" "bar" "baz"]}))))

 (testing "nested maps in values"
          (is (= "Foo: fooness  Bar: barness"
                 (render-template (StringTemplate.
                                   "Foo: $user.foo$  Bar: $user.bar$")
                                  {:user {:foo "fooness" :bar "barness"}}))))
 (testing "seq of nested maps in values"
          (is (= "Foo: f1f2f3"
                 (render-template (StringTemplate.
                                   "Foo: $user:{f$it.foo$}$")
                                  {:user [{:foo 1} {:foo 2} {:foo 3}]})))))

(defn www-templates-dir []
  (some
   (fn [dir]
     (let [template-dir
           (java/file dir "org" "altlaw" "www" "templates")]
       (when (.exists template-dir) template-dir)))
   (cp/classpath-directories)))

(def #^{:private true} template-group
     (sing/global-singleton
      (fn []
        (let [page-templates (StringTemplateGroup. "org.altlaw.www.templates"
                                                   (str (www-templates-dir)))]
          ;; By default, templates are never refreshed.
          ;; In development mode, always refresh.
          (when (= (context/altlaw-env) "development")
            (.setRefreshInterval page-templates 0))
          page-templates))))

(with-test
 (defn render
   "Renders a template from a file.  name does not include the '.st'
   extension.  attrs is a :keyword=>value map as with
   render-template."
   [name & attrs]
   (-> (template-group)
       (.getInstanceOf (java/as-str name))
       (render-template (if (map? (first attrs))
                          (if (next attrs)
                            (apply assoc (first attrs) (next attrs))
                            (first attrs))
                          (apply array-map attrs)))))

 (is (= "<body>\nHello, Harry.\n</body>"
        (render "test1" {:name "Harry"})))
 (is (= "<body>\nHello, Harry.\n</body>"
        (render :test1 :name "Harry")))
 (is (= "<body>\nHello, .\n</body>"
        (render :test1))))


(defn h
  "Converts x to String and escapes HTML using XML entities."
  [x]
  (StringEscapeUtils/escapeXml (str x)))
