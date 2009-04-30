(ns org.altlaw.test.jruby
  (:use clojure.contrib.test-is
        org.altlaw.util.jruby))

(deftest test-conversion-types
  (is (instance? java.lang.Number
                 (convert-jruby (eval-jruby "3.14"))))
  (is (instance? java.lang.Boolean
                 (convert-jruby (eval-jruby "true"))))
  (is (instance? java.lang.String
                 (convert-jruby (eval-jruby "'foo'")))))

(deftest test-date-conversion
  (eval-jruby "require 'date'")
  (is (instance? java.lang.String (convert-jruby (eval-jruby "Date.civil(2009,1,1)"))))
  (is (= "2009-01-01" (convert-jruby (eval-jruby "Date.civil(2009,1,1)")))))

(deftest test-simple-conversion
  (is (= true (convert-jruby (eval-jruby "true"))))
  (is (= false (convert-jruby (eval-jruby "false"))))
  (is (nil? (convert-jruby (eval-jruby "nil"))))
  (is (= :foo (convert-jruby (eval-jruby ":foo")))))

(deftest test-array-conversion
  (is (vector? (convert-jruby (eval-jruby "[1,2,3]"))))
  (is (= [] (convert-jruby (eval-jruby "[]"))))
  (is (= [1 2 3] (convert-jruby (eval-jruby "[1,2,3]")))))

(deftest test-hash-conversion
  (is (map? (convert-jruby (eval-jruby "{}"))))
  (is (map? (convert-jruby (eval-jruby "{'a'=>1, 'b'=>2}"))))
  (is (= {:a 1, :b 2} (convert-jruby (eval-jruby "{:a=>1, :b=>2}"))))
  (is (= {:b 2, :a 1} (convert-jruby (eval-jruby "{:a=>1, :b=>2}"))))
  (is (= {3 30 4 40} (convert-jruby (eval-jruby "{3=>30,4=>40}")))))
