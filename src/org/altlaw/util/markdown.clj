(ns org.altlaw.util.markdown
  (:use [clojure.contrib.test-is :only (with-test is)])
  (:import (com.petebevin.markdown MarkdownProcessor)))

(with-test
    (defn markdown [text]
      (.markdown (MarkdownProcessor.) text))
  (is (= "<p>Hello, World!</p>\n"
         (markdown "Hello, World!")))
  (is (= "<p>This &amp; that.</p>\n"
         (markdown "This & that.")))
  (is (= "<p>This &amp; that.</p>\n"
         (markdown "This &amp; that."))))
