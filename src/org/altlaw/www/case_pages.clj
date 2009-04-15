(ns org.altlaw.www.case-pages
  (:require [org.altlaw.util.string :as str]
            [org.altlaw.www.render :as tmpl]
            [org.altlaw.util.courts :as courts]
            [org.altlaw.util.date :as date]
            [org.altlaw.db.privacy :as priv]
            [clojure.contrib.duck-streams :as duck])
  (:use [clojure.contrib.test-is :only (deftest- with-test is)]))


;;; SHARED FUNCTIONS FOR ALL PAGE TYPES

(defn- escape-fields [doc]
  (reduce (fn [m [k v]]
            (assoc m k 
                   (cond
                    (= :html k) v
                    (= :date k) (date/format-long-date v)
                    (= :court k) (courts/*court-names* v)
                    (#{:name} k) (tmpl/h v)
                    (#{:citations :dockets} k) (map tmpl/h v)
                    :else v)))
          {} doc))

(defn- url-path-for-docid [type docid]
  (if (= type :text)
    (str "v1/cases/" docid)
    (str "v1/cases/" docid "/" (name type))))

(defn- absolute-url-path-for-docid [type docid]
  (str "/" (url-path-for-docid type docid)))

(defn- file-path-for-docid [type docid format]
  (str (url-path-for-docid type docid) "." format))

(defn- doclinks [doc]
  (let [docid (:docid doc)]
    [{:name "Full Text"
      :url (absolute-url-path-for-docid :text docid)}
     {:name "Citations to/from this"
      :url (absolute-url-path-for-docid :citations docid)}]))

(defmulti #^{:private true} gen-case-layout (fn [type doc] type))



;;; TEXT PAGES

(defmethod gen-case-layout :text [type doc]
  (tmpl/render :default_layout
               :page_class (str "doctype_" (:doctype doc))
               :content_head (tmpl/render :case_head_layout doc)
               :content_body (:html doc)
               :sidebar (tmpl/render :case_sidebar
                                     :doclinks (doclinks doc))))


;;; CITATION PAGES

(defn- prepare-citelinks [cites]
  (map (fn [doc]
         (assoc (escape-fields doc)
           :url (absolute-url-path-for-docid :text (:docid doc))
           :name (str/truncate (:name doc) 30)))
       (reverse (sort-by :date cites))))
  

(defmethod gen-case-layout :citations [type doc]
  (let [in (prepare-citelinks (:incites doc))
        out (prepare-citelinks (:outcites doc))]
    (tmpl/render :default_layout
                 :page_class (str "doctype_" (:doctype doc))
                 :content_head (tmpl/render :case_head_layout doc)
                 :content_body (tmpl/render :citelinks
                                            :incites in
                                            :outcites out)
                 :sidebar (tmpl/render :case_sidebar
                                       :doclinks (doclinks doc)))))


;;; GENERATE ALL PAGES

(defn gen-case-page [type doc]
  (let [escaped (escape-fields doc)]
    (tmpl/render :xhtml_page
                 :html_title (str (:name escaped) " - AltLaw")
                 :html_head (tmpl/render :default_html_head
                                         :norobots (or (= type :citations)
                                                       (priv/norobots? (:docid doc))))
                 :html_body (gen-case-layout type escaped))))

(defn all-files [doc]
  (reduce (fn [m type]
            (assoc m (file-path-for-docid type (:docid doc) "html")
                   (gen-case-page type doc)))
          {} [:text :citations]))


;;; TESTS

(deftest- can-generate-case-text
  (let [page (gen-case-page :text
                            {:docid 1 :doctype "case"
                             :name "John & Sons v. Thurber"
                             :citations ["1 F.2d 101" "5 L.Ed. 102"]
                             :date "2009-01-04"
                             :court "http://id.altlaw.org/courts/us/fed/app/3"})]
    (is (re-find #"class=\"[^\"]*doctype_case[^\"]*\"" page))
    (is (re-find #"<title>John &amp; Sons v\. Thurber.*</title>" page))
    (is (re-find #"<.*class=\"name\".*>John &amp; Sons v\. Thurber</" page))
    (is (re-find #"<.*class=\"date\".*>January 4, 2009</" page))
    (is (re-find #"<.*class=\"citations\".*>1 F.2d 101; 5 L.Ed. 102</" page))
    (is (re-find #"<.*class=\"court\".*>United States Court of Appeals for the Third Circuit</" page))
    (is (not (re-find #"robots.*noindex" page)))))


(deftest- can-generate-cites-page
  (let [page (gen-case-page :citations
                            {:docid 1 :doctype "case"
                             :name "John & Sons v. Thurber"
                             :citations ["1 F.2d 101" "5 L.Ed. 102"]
                             :date "2000-01-04"
                             :court "http://id.altlaw.org/courts/us/fed/app/3"
                             :incites [{:docid 2079
                                        :court "http://id.altlaw.org/courts/us/fed/app/5"
                                        :date "2006-02-19"
                                        :name "Bob v. Marley"}
                                       {:docid 3079
                                        :court "http://id.altlaw.org/courts/us/fed/app/7"
                                        :date "2006-02-21"
                                        :name "John v. Marley"}]})]
    (is (re-find #"Bob v. Marley" page))
    (is (re-find #"/v1/cases/2079" page))
    (is (re-find #"February 19, 2006" page))
    (is (re-find #"United States Court of Appeals for the Fifth Circuit" page))
    (is (re-find #"robots.*noindex" page))))