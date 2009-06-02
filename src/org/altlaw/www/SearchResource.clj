(ns org.altlaw.www.SearchResource
  (:require [org.altlaw.www.render :as tmpl]
            [org.altlaw.www.references :as r]
            [org.altlaw.www.pagination :as p]
            [org.altlaw.util.date :as date]
            [org.altlaw.util.courts :as courts]
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


;;; RENDERING RESULTS

(defn get-snippets [highlights docid]
  (when-let [h (get highlights (str docid))]
    (get h "text")))

(defn prepare-hit [this highlights doc]
  (let [docid (.getFieldValue doc "docid")
        name (.getFieldValue doc "name")
        the-name (if (or (nil? name) (empty? name))
                   (str "Untitled #" (.getFieldValue doc "docid"))
                   name)
        date (.getFieldValue doc "date")]
  {:docid docid
   :url (str (.getTargetRef (Reference. (.. this getRequest getOriginalRef)
                                        (str "/v1/cases/" docid))))
   :timestamp (if (seq date) (str date "T10:00:00-05:00")
                  (DateUtils/timestamp))
   :size (when-let [size (.getFieldValue doc "size")]
           (int (Math/ceil (/ size 1024.0))))
   :date (date/format-long-date date)
   :court (courts/*court-names* (.getFieldValue doc "court"))
   :name (tmpl/h the-name)
   :citations (map tmpl/h (.getFieldValues doc "citations"))
   :snippets (get-snippets highlights docid)}))

(defn prepare-solr-results [this solr-response]
  (let [docs (.getResults solr-response)
        query (.. solr-response getHeader (get "params") (get "q"))]
    {:query (tmpl/h query)
     :sort (.. solr-response getHeader (get "params") (get "sort"))
     :total_hits (.getNumFound docs)
     :start_hit (if (zero? (.getNumFound docs)) 0 (inc (.getStart docs)))
     :end_hit (+ (.getStart docs) (count docs))
     :hits (map (partial prepare-hit this (.getHighlighting solr-response))
                docs)}))

(defn get-current-page [ref]
  (let [params (.. ref getQueryAsForm getValuesMap)]
    (if-let [p (get params "page")]
      (Integer/parseInt p)
      1)))

(defn order-for-display [sort]
  (cond
   (= sort "score desc") "relevance"
   (= sort "date desc") "date"
   :else "unknown order"))

(defn make-sortlinks [ref sort]
  (cond
   (= sort "score desc") [{:url (r/sort-ref ref "date") :name "Sort by date"}]
   (= sort "date desc") [{:url (r/sort-ref ref "relevance") :name "Sort by relevance"}]))

(defn prepare-atom-result-url [ref data]
  (assoc data :atom_results_url
         (tmpl/h (-> ref
                     (r/page-ref 1)
                     (r/format-ref "atom")
                     (r/sort-ref "date")))))

(defn prepare-html-template [ref data]
  (let [data (prepare-atom-result-url ref data)]
    ;; Do not display "relevance" sort link when using 'all' query
    (if (= (:query data) "docid:[* TO *]")
      data
      (assoc data
        :order (order-for-display (:sort data))
        :sortlinks (make-sortlinks ref (:sort data))))))

(defn html-results [ref data]
  (let [data (prepare-html-template ref data)
        current-page (get-current-page ref)
        pagination (when (> (:total_hits data) p/*page-size*)
                     (p/pagination-html ref current-page (:total_hits data)))]
    (tmpl/render "search/html_results"
                 (assoc data :pagination pagination
                        :current_page current-page))))

(defmulti render-results (fn [this variant data] (.getMediaType variant)))

(defmethod render-results MediaType/TEXT_HTML [this variant data]
  (let [ref (.. this getRequest getOriginalRef)]
    (StringRepresentation.
     (html-results ref data)
     MediaType/TEXT_HTML
     Language/ENGLISH_US
     CharacterSet/UTF_8)))

(defn prepare-atom-fields [this data]
  (let [ref (.. this getRequest getOriginalRef)
        atom-ref (r/format-ref ref "atom")
        current-page (get-current-page ref)
        last-page (p/calc-last-page (:total_hits data) p/*page-size*)
        next-page (when (< current-page last-page) (inc current-page))
        prev-page (when (> current-page 1) (dec current-page))]
    (assoc data
      :order (order-for-display (:sort data))
      :timestamp (DateUtils/timestamp)
      :current_page current-page
      :page_size p/*page-size*
      :html_results_url (tmpl/h ref)
      :atom_results_url (tmpl/h atom-ref)
      :atom_first_page_url (tmpl/h (r/page-ref atom-ref 1))
      :atom_last_page_url (tmpl/h (r/page-ref atom-ref last-page))
      :atom_prev_page_url (when prev-page (tmpl/h (r/page-ref atom-ref prev-page)))
      :atom_next_page_url (when next-page (tmpl/h (r/page-ref atom-ref next-page)))
      :atom_id (str "urn:uuid:" (java.util.UUID/randomUUID)))))

(defmethod render-results MediaType/APPLICATION_ATOM_XML [this variant data]
  (StringRepresentation.
   (tmpl/render "search/atom_results" (prepare-atom-fields this data))
   MediaType/APPLICATION_ATOM_XML
   Language/ENGLISH_US
   CharacterSet/UTF_8))


;;; RENDERING EMPTY RESULT SET

(defmulti render-no-results
  (fn [this variant prepared] (.getMediaType variant)))

(defmethod render-no-results MediaType/TEXT_HTML [this variant prepared]
  (StringRepresentation.
   (tmpl/render "search/html_no_results" prepared)
   MediaType/TEXT_HTML
   Language/ENGLISH_US
   CharacterSet/UTF_8))

(defmethod render-no-results MediaType/APPLICATION_ATOM_XML [this variant prepared]
  (StringRepresentation.
   (tmpl/render "search/atom_results"
                (prepare-atom-fields this prepared))
   MediaType/TEXT_HTML
   Language/ENGLISH_US
   CharacterSet/UTF_8))


;;; RENDERING BLANK FORMS

(defn render-blank-form [this variant search-type]
  (StringRepresentation.
   (tmpl/render (str "search/" (name search-type) "_search_page"))
   MediaType/TEXT_HTML
   Language/ENGLISH_US
   CharacterSet/UTF_8))


;;; RENDERING EXCEPTIONS

(defn html-exception [data]
  (tmpl/render "search/error_page"
               :error_message
               (if (= "development" (context/altlaw-env))
                 (with-out-str
                   (print "<pre>")
                   (stacktrace/print-cause-trace (:exception data) 15)
                   (print "</pre>"))
                 (.getMessage (stacktrace/root-cause (:exception data))))))

(defn render-exception [this variant data]
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
    (.setStart solr-query (* p/*page-size* (dec page)))))

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

(defn apply-highlight-options [solr-query]
  (doto solr-query
    (.setHighlight true)
    (.setHighlightFragsize 120)
    (.setHighlightRequireFieldMatch true)
    (.setHighlightSnippets 3)))

(defmulti get-solr-query (fn [this type] type))

(defmethod get-solr-query :simple [this type]
  (let [params (get-query-params this)]
    (when-let [q (clean-param (.get params "q"))]
      (log this "Preparing simple search for " (pr-str q))
      (doto (SolrQuery. q)
        (.setQueryType "dismax")
        (apply-sort-options this)
        (apply-highlight-options)))))

(defmethod get-solr-query :boolean [this type]
  (let [params (get-query-params this)]
    (when-let [q (clean-param (.get params "q"))]
      (log this "Preparing boolean search for " (pr-str q))
      (doto (SolrQuery. q)
        (.setQueryType "standard")
        (apply-sort-options this)
        (apply-highlight-options)))))

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
  [(Variant. MediaType/TEXT_HTML) (Variant. MediaType/APPLICATION_ATOM_XML)])

(defn -represent [this variant]
  (try
   (if-let [search-type (get-search-type this)]
     (if-let [solr-query (get-solr-query this search-type)]
       (let [solr-results (execute-search this solr-query)
             prepared (prepare-solr-results this solr-results)]
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
