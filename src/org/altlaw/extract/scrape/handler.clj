(ns org.altlaw.extract.scrape.handler
  (:require [org.altlaw.util.jruby :as ruby]
            [clojure.contrib.walk :as walk]
            [clojure.contrib.singleton :as sing]))

(def scraper-handler
     (sing/per-thread-singleton
      (fn []
        (ruby/eval-jruby "require 'org/altlaw/extract/scrape/scraper_handler'")
        (ruby/eval-jruby "ScraperHandler.new"))))

(defn run-scrapers [download]
  (assert (map? download))
  (assert (contains? download :request_uri))
  (map ruby/convert-jruby
       (ruby/eval-jruby "$handler.parse(Download.from_map($download))"
                        {:download (walk/stringify-keys download)
                         :handler (scraper-handler)})))

(defn all-requests []
  (map ruby/convert-jruby
       (ruby/eval-jruby "$handler.all_requests"
                        {:handler (scraper-handler)})))
