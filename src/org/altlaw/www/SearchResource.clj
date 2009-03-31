(ns org.altlaw.www.SearchResource
  (:require [org.altlaw.pages.templates :as tmpl]
            [org.altlaw.util.date :as date]
            [org.altlaw.util :as util]
            [org.altlaw.util.context :as context]
            [clojure.contrib.stacktrace :as stacktrace])
  (:use [clojure.contrib.str-utils :only (str-join)])
  (:gen-class :extends org.restlet.resource.Resource)
  (:import (org.restlet.resource Variant StringRepresentation ResourceException)
           (org.restlet.data MediaType Language CharacterSet Reference Status)
           (org.apache.solr.client.solrj SolrQuery SolrQuery$ORDER)
           (org.apache.commons.lang StringUtils)
           (org.altlaw.util DateUtils)))

;;; UTILITIES 

(defn get-query-params [this]
  (.. this getRequest getResourceRef getQueryAsForm getValuesMap))

(defn clean-param [param-string]
  (when param-string (.trim (Reference/decode param-string))))

(defn log [this & args]
  (.. this getContext getLogger (info (apply str args))))

;;; PAGE NUMBERING

(def *page-size* 10)

(def *window-size* 5)

(defn page-ref [ref number]
  (let [newref (Reference. ref)
        query (.getQueryAsForm newref)]
    (.set query "page" (str number) true)
    (.setQuery newref (.encode query))
    newref))

(defn pagination-html [ref current total-hits]
  (let [last-page (int (Math/ceil (/ total-hits (float *page-size*))))
        window-start (if (< (- current *window-size*) 1)
                       1 (- current *window-size*))
        window-end (if (> (+ current *window-size*) last-page)
                     last-page (+ current *window-size*))]
    (with-out-str
      (print "<div class=\"pagination\"><ul>")
      (print "<li>Pages:</li>")
      (if (= current 1)
        (print "<li class=\"disablepage\">&#171; previous</li>")
        (printf "<li class=\"prevpage\"><a href=\"%s\">&#171; previous</a></li>"
                (str (page-ref ref (dec current)))))
      (when (not= window-start 1)
        (printf "<li><a href=\"%s\">1</a></li>" (str (page-ref ref 1)))
        (print "<li>...</li>"))
      (doseq [n (range window-start (inc window-end))]
        (if (= n current)
          (printf "<li class=\"currentpage\">%d</li>" n)
          (printf "<li><a href=\"%s\">%d</a></li>" (str (page-ref ref n)) n)))
      (when (not= window-end last-page)
        (print "<li>...</li>")
        (printf "<li><a href=\"%s\">%d</a></li>"
                (str (page-ref ref last-page)) last-page))
      (if (= current last-page)
        (print "<li class=\"disablepage\">next &#187;</li>")
        (printf "<li class=\"prevpage\"><a href=\"%s\">next &#187;</a></li>"
                (str (page-ref ref (inc current)))))
      (print "</ul></div>"))))

;;; RENDERING RESULTS

(defn prepare-hit [doc]
  (let [name (.getFieldValue doc "name")
        the-name (if (or (nil? name) (empty? name))
                   (str "Untitled #" (.getFieldValue doc "docid"))
                   name)]
  {:docid (.getFieldValue doc "docid")
   :url (format "/v1/cases/%s" (.getFieldValue doc "docid"))
   :size (when-let [size (.getFieldValue doc "size")]
           (int (Math/ceil (/ size 1024.0))))
   :date (date/format-long-date (.getFieldValue doc "date"))
   :court (util/*court-names* (.getFieldValue doc "court"))
   :name (tmpl/h the-name)
   :citations (map tmpl/h (.getFieldValues doc "citations"))}))

(defn prepare-solr-results [solr-response]
  (let [docs (.getResults solr-response)]
    {:query (.. solr-response getHeader (get "params") (get "q"))
     :sort (.. solr-response getHeader (get "params") (get "sort"))
     :total_hits (.getNumFound docs)
     :start_hit (if (zero? (.getNumFound docs)) 0 (inc (.getStart docs)))
     :end_hit (+ (.getStart docs) (count docs))
     :hits (map prepare-hit docs)}))

(defn html-exception-layout [data]
  (let [root-cause (loop [e (:exception data)]
                     (if-let [cause (.getCause e)]
                       (recur cause) e))]
    (tmpl/render :default_layout
                 :page_class "doctype_search"
                 :content_head "<h1>Search Error</h1>"
                 :content_body (tmpl/render
                                :error :message
                                (if (= "development" (context/altlaw-env))
                                  (with-out-str
                                    (print "<pre>")
                                    (stacktrace/print-cause-trace root-cause 15)
                                    (print "</pre>"))
                                  (.getMessage root-cause))))))

(defn html-exception [data]
  (tmpl/render :xhtml_page
               :html_title "Search Error - AltLaw"
               :html_head (tmpl/render :default_html_head)
               :html_body (html-exception-layout data)))

(defn html-results-layout [data]
  (tmpl/render :default_layout
               :page_class "doctype_search"
               :content_head (tmpl/render :html_search_results_head data)
               :content_body (tmpl/render :html_search_results_body data)
               :sidebar ""))

(defn html-results [ref data]
  (let [params (.. ref getQueryAsForm getValuesMap)
        current-page (Integer/parseInt (or (get params "page") "1"))
        pagination (when (> (:total_hits data) *page-size*)
                     (pagination-html ref current-page (:total_hits data)))]
    (tmpl/render :xhtml_page
                 :html_title "Search Results - AltLaw"
                 :html_head (tmpl/render :default_html_head)
                 :html_body (html-results-layout (assoc data
                                                   :pagination pagination)))))

(defn html-no-results-layout [data]
  (tmpl/render :default_layout
               :page_class "doctype_search"
               :content_head (tmpl/render :html_search_results_head data)
               :content_body "<p>No search results.</p>"))

(defn html-no-results [data]
  (tmpl/render :xhtml_page
               :html_title "No Results - AltLaw"
               :html_head (tmpl/render :default_html_head)
               :html_body (html-no-results-layout data)))

(defn order-for-display [sort]
  (cond
   (= sort "score desc") "relevance"
   (= sort "date desc") "date"
   :else "unknown order"))

(defn sort-ref [ref sort]
  (let [newref (Reference. ref)
        query (.getQueryAsForm newref)]
    (.set query "sort" sort true)
    (.setQuery newref (.encode query))
    newref))

(defn make-sortlinks [ref sort]
  (cond
   (= sort "score desc") [{:url (sort-ref ref "date") :name "Sort by date"}]
   (= sort "date desc") [{:url (sort-ref ref "relevance") :name "Sort by relevance"}]))

(defmulti render-results (fn [this variant data] (.getMediaType variant)))

(defmethod render-results MediaType/TEXT_HTML [this variant data]
  (let [ref (.. this getRequest getOriginalRef)
        data (if (= (:query data) "docid:[* TO *]")
               data
               (assoc data
                 :order (order-for-display (:sort data))
                 :sortlinks (make-sortlinks ref (:sort data))))]
    (StringRepresentation.
     (cond
      (contains? data :exception) (html-exception data)
      (zero? (:total_hits data)) (html-no-results data)
      :else (html-results ref data))
     MediaType/TEXT_HTML
     Language/ENGLISH_US
     CharacterSet/UTF_8)))


(defn render-no-results [this variant prepared]
  (StringRepresentation.
   (html-no-results prepared)
     MediaType/TEXT_HTML
     Language/ENGLISH_US
     CharacterSet/UTF_8))


;;; RENDERING BLANK FORMS

(defn html-form-layout [query-type]
  (let [type (name query-type)]
    (tmpl/render :default_layout
                 :page_class "doctype_search"
                 :content_head (str "<h1>" (StringUtils/capitalize type)
                                    " Search</h1>")
                 :content_body (tmpl/render (str type "_search_page")))))

(defn html-form [query-type]
  (tmpl/render :xhtml_page
               :html_title "Search - AltLaw"
               :html_head (tmpl/render :advanced_search_html_head)
               :html_body (html-form-layout query-type)))


(defmulti render-blank-form (fn [this variant search-type] (.getMediaType variant)))

(defmethod render-blank-form MediaType/TEXT_HTML [this variant search-type]
  (StringRepresentation.
   (html-form search-type)
   MediaType/TEXT_HTML
   Language/ENGLISH_US
   CharacterSet/UTF_8))


;;; RENDERING EXCEPTIONS

(defmulti render-exception (fn [this variant data] (.getMediaType variant)))

(defmethod render-exception MediaType/TEXT_HTML [this variant data]
  (StringRepresentation.
   (html-exception data)
   MediaType/TEXT_HTML
   Language/ENGLISH_US
   CharacterSet/UTF_8))


;;; SEARCHING

(def *default-fields*
     (into-array ["doctype" "docid" "name" "citations"
                  "date" "court" "size"]))



(defn apply-page-options [this solr-query]
  (let [params (get-query-params this)
        page (if-let [p (.get params "page")]
               (Integer/parseInt p) 1)]
    (.setStart solr-query (* *page-size* (dec page)))))

(defn execute-search [this solr-query]
  (let [solr (.. this getContext getAttributes (get "org.altlaw.solr.server"))]
    (do (.setFields solr-query *default-fields*)
        (apply-page-options this solr-query)
        (.query solr solr-query))))


;;; PREPARING THE SOLR QUERY

(defn get-sort-field [this]
  (let [params (get-query-params this)
        sort (clean-param (.get params "sort"))]
    (cond
     (or (nil? sort) (= sort "relevance")) "score"
     (= sort "date") "date"
     :else (throw (ResourceException. Status/CLIENT_ERROR_BAD_REQUEST
                                      "Invalid sort field.")))))

(defn apply-sort-options [solr-query this]
  (.setSortField solr-query (get-sort-field this) SolrQuery$ORDER/desc))

(defmulti get-solr-query (fn [this type] type))

(defmethod get-solr-query :simple [this type]
  (let [params (get-query-params this)]
    (when-let [q (clean-param (.get params "q"))]
      (log this "Preparing simple search for " (pr-str q))
      (doto (SolrQuery. q)
        (.setQueryType "dismax")
        (apply-sort-options this)))))

(defmethod get-solr-query :boolean [this type]
  (let [params (get-query-params this)]
    (when-let [q (clean-param (.get params "q"))]
      (log this "Preparing boolean search for " (pr-str q))
      (doto (SolrQuery. q)
        (.setQueryType "standard")
        (apply-sort-options this)))))

(defmethod get-solr-query :citation [this type]
  (let [params (get-query-params this)]
    (when-let [q (clean-param (.get params "q"))]
      (log this "Preparing citation search for " (pr-str q))
      (doto (SolrQuery. (str "citations:\"" q "\""))
        (.setQueryType "standard")
        (apply-sort-options this)))))

(defmethod get-solr-query :all [this type]
  (log this "Preparing ALL search.")
  (doto (SolrQuery. "docid:[* TO *]")
    (.setQueryType "standard")
    ;; Always sort by date on ALL search:
    (.setSortField "date" SolrQuery$ORDER/desc)))



;;; HANDLING ADVANCED QUERY FORM

(defn tokenize [str]
  (seq (.split #"\s+" str)))

(defn format-advanced-all [params]
  (when (seq (:all params))
    (:all params)))

(defn format-advanced-any [params]
  (when (seq (:any params))
    (str "("
         (str-join " OR "
                   (tokenize (:any params)))
         ")")))

(defn format-advanced-none [params]
  (when (seq (:none params))
    (str "NOT ("
         (str-join " OR "
                   (tokenize (:none params)))
         ")")))

(defn format-advanced-name [params]
  (when (seq (:title params))
    (str "name:(" (:title params) ")")))

(defn format-advanced-phrase [params]
  (when (seq (:phrase params))
    (str \" (:phrase params) \")))

(defn format-advanced-near [params]
  (when (seq (:near params))
    (let [slop (if (seq (:distance params))
                 (:distance params) "5")]
      (str \" (:near params) "\"~" slop))))

(defn get-advanced-params [params]
  (reduce (fn [m [k v]]
            (assoc m
              (keyword (.. k (replace "advanced[" "") (replace "]" "")))
              v))
          {} params))

(def *court-uris*
     (array-map :supreme "http://id.altlaw.org/courts/us/fed/supreme" 
                :ca1 "http://id.altlaw.org/courts/us/fed/app/1"
                :ca2 "http://id.altlaw.org/courts/us/fed/app/2"
                :ca3 "http://id.altlaw.org/courts/us/fed/app/3"
                :ca4 "http://id.altlaw.org/courts/us/fed/app/4"
                :ca5 "http://id.altlaw.org/courts/us/fed/app/5"
                :ca6 "http://id.altlaw.org/courts/us/fed/app/6"
                :ca7 "http://id.altlaw.org/courts/us/fed/app/7"
                :ca8 "http://id.altlaw.org/courts/us/fed/app/8"
                :ca9 "http://id.altlaw.org/courts/us/fed/app/9"
                :ca10 "http://id.altlaw.org/courts/us/fed/app/10"
                :ca11 "http://id.altlaw.org/courts/us/fed/app/11" 
                :cafc "http://id.altlaw.org/courts/us/fed/app/fed"
                :cadc "http://id.altlaw.org/courts/us/fed/app/dc"))

(defn format-advanced-courts [params]
  (let [court-params (select-keys params (keys *court-uris*))]
    (if (or (every? #(= "1" %) (vals court-params))
            (every? #(= "0" %) (vals court-params)))
      ;; If all checked or all unchecked, ignore:
      nil
      ;; Otherwise, add the court URIs
      (str "court:("
           (str-join " OR "
                     (filter identity
                             (map (fn [[k v]]
                                    (when (= "1" v)
                                      (str \" (*court-uris* k) \")))
                                  court-params)))
           ")"))))

(defn parse-date [string at-start?]
  (if (re-matches #"\d{4}" string)
    (if at-start?
      (str string "-01-01")
      (str string "-12-31"))
    (DateUtils/sqlDateString (DateUtils/parsePastDate string))))

(defn parse-advanced-dates [start-string finish-string]
  (let [start-date (parse-date start-string true)
        finish-date (parse-date finish-string false)]
    ;; If we failed to parse either one, return null:
    (when (and start-date finish-date)
      ;; If user entered dates backwards, swap them:
      (if (pos? (compare start-date finish-date))
        (parse-advanced-dates finish-string start-string)
        ;; Otherwise, return both:
        [start-date finish-date]))))

(defn format-advanced-dates [params]
  (let [start-param (:date_start params)
        finish-param (:date_end params)]
    (when (or (seq start-param) (seq finish-param))
      ;; If only one date is entered, use it for both start and end
      ;; dates, interpreted as a range.
      (let [start-string (if (seq start-param) start-param finish-param)
            finish-string (if (seq finish-param) finish-param start-param)]
        (when-let [parsed (parse-advanced-dates start-string finish-string)]
          (let [[start finish] parsed]
            (str "date:[" start " TO " finish "]")))))))

(defn make-advanced-query-string [params]
  (str-join " AND "
            (filter identity
                    (list (format-advanced-all params)
                          (format-advanced-any params)
                          (format-advanced-none params)
                          (format-advanced-phrase params)
                          (format-advanced-near params)
                          (format-advanced-name params)
                          (format-advanced-courts params)
                          (format-advanced-dates params)))))

(defmethod get-solr-query :advanced [this type]
  (let [params (get-advanced-params (get-query-params this))]
    (log this "Preparing advanced search with params " (pr-str params))
    (let [query-string (make-advanced-query-string params)]
      (when (seq query-string)
        (doto (SolrQuery. query-string)
          (.setQueryType "standard")
          (apply-sort-options this))))))



;;; DETERMINING SEARCH TYPE

(def *citation-regex* #"(?i)^\d+\s+[A-Za-z0-9.'-]+\s+\d+$")

(defn get-search-type [this]
  (let [type (.. this getRequest getAttributes (get "query_type"))]
    (log this "query_type URI parameter is " (pr-str type))
    (if (and type (seq type))
      (when (#{"advanced" "boolean"} type) (keyword type))
      ;; Simple search, but maybe a special kind:
      (let [params (get-query-params this)]
        (let [q (str (clean-param (.get params "q")))]
          (cond (= q "*") :all
                (re-matches *citation-regex* q) :citation
                :else :simple))))))



;;; REDIRECTION

(defn redirect-to-document [this docid]
  (let [ref (Reference. (.. this getRequest getOriginalRef))]
    (.setPath ref (str "/v1/cases/" docid))
    (.setQuery ref nil)
    (.. this getResponse (redirectSeeOther ref))))

;;; RESOURCE METHODS

(defn -isModifiable [this] false)

(defn -getVariants [this]
  [(Variant. MediaType/TEXT_HTML)])

(defn -represent [this variant]
  (try
   (if-let [search-type (get-search-type this)]
     (if-let [solr-query (get-solr-query this search-type)]
       (let [solr-results (execute-search this solr-query)
             prepared (prepare-solr-results solr-results)]
         (if (and (= :citation search-type)
                  (= 1 (:total_hits prepared)))
           ;; Citation search, go straight to document:
           (redirect-to-document this (:docid (first (:hits prepared))))
           ;; Normal result rendering:
           (if (pos? (:total_hits prepared))
             (render-results this variant prepared)
             (render-no-results this variant prepared))))
       ;; No query:
       (render-blank-form this variant search-type))
     ;; Unknown search type (bad URI):
     (throw (ResourceException. Status/CLIENT_ERROR_NOT_FOUND)))
   (catch org.apache.solr.client.solrj.SolrServerException e
     (render-exception this variant {:exception e}))))
