(ns org.altlaw.util.solr
  (:import (org.apache.solr.core SolrCore SolrConfig SolrResourceLoader CoreContainer CoreDescriptor)
           (org.apache.solr.common SolrInputDocument)
           (org.apache.solr.schema IndexSchema)
           (org.apache.solr.client.solrj.embedded EmbeddedSolrServer)
           (org.apache.commons.io IOUtils)
           (java.io File FileOutputStream)))

(defn- write-solr-conf-resource [conf-dir name]
  (let [loader (.getContextClassLoader (Thread/currentThread))
        resource (str "org/altlaw/solr/conf/" name)]
    (with-open [in (.getResourceAsStream loader resource)
                out (FileOutputStream. (File. conf-dir name))]
      (IOUtils/copy in out))))

(def #^{:private true} *solr-config-files*
     ["court_synonyms.txt" "elevate.xml" "protwords.txt"
      "schema.xml" "scripts.conf" "solrconfig.xml"
      "stopwords.txt" "synonyms.txt"])

(defn- setup-solr-home [#^File dir]
  ;; Create directories
  (let [data-dir (File. dir "data")
        conf-dir (File. dir "conf")]
    (.mkdirs data-dir)
    (.mkdirs conf-dir)
    (doseq [f *solr-config-files*]
      (write-solr-conf-resource conf-dir f))))

(defn start-embedded-solr
  "Starts an embedded Solr server.  dir will be created, and Solr's
  configuration files copied to the \"conf\" subdirectory.  Returns a
  vector, [server core], where server is an EmbeddedSolrServer and
  core is a SolrCore."
  [#^File dir]
  (setup-solr-home dir)
  (let [home-dir (str dir)
        data-dir (str dir "/data")
        core-name (str (gensym "solr_core_"))
        loader (SolrResourceLoader. home-dir)
        cc (CoreContainer. loader)
        cd (CoreDescriptor. cc core-name home-dir)
        config (SolrConfig. home-dir "solrconfig.xml" nil)
        schema (IndexSchema. config "schema.xml" nil)
        core (SolrCore. core-name data-dir config schema cd)]
    (.register cc core-name core false)
    (let [server (EmbeddedSolrServer. core)]
      [server core])))

(defn make-solr-document
  "Creates a SolrInputDocument from a keyword=>value map.  Values which are
  collections will be stored as multi-valued fields."
  [data-map]
  (let [sd (SolrInputDocument.)]
    (doseq [[field value] data-map]
      (if (coll? value)
        (doseq [v value]
          (.addField sd (name field) (str v)))
        (.addField sd (name field) (str value))))
    sd))

(defn stop-embedded-solr
  "Shuts down an embedded Solr server and core, after commiting and
  optimizing the index."
  [server core]
  (.commit server)
  (.optimize server)
  (.close core))
