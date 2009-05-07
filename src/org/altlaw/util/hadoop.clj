(ns org.altlaw.util.hadoop
  (:require [org.altlaw.util.log :as log]
            [clojure.contrib.json.read :as json]
            [clojure.contrib.walk :as walk]))

(defn import-hadoop
  []
  (import
   '(org.apache.hadoop.conf Configuration Configured)
   '(org.apache.hadoop.fs Path FileSystem FileUtil FSInputStream FSDataInputStream FSDataOutputStream)
   '(org.apache.hadoop.filecache DistributedCache)
   '(org.apache.hadoop.io BytesWritable IntWritable LongWritable MD5Hash SequenceFile SequenceFile$CompressionType Text VIntWritable VLongWritable NullWritable WritableUtils)
   '(org.apache.hadoop.io.compress GzipCodec)
   '(org.apache.hadoop.mapred FileInputFormat FileOutputFormat JobClient JobConf SequenceFileInputFormat SequenceFileOutputFormat SequenceFileAsTextInputFormat TextOutputFormat Mapper Reducer Reporter KeyValueTextInputFormat TextInputFormat Counters OutputCollector)
   '(org.apache.hadoop.mapred.lib IdentityMapper IdentityReducer LongSumReducer)
   '(org.apache.hadoop.util Tool ToolRunner)
   '(org.apache.commons.logging Log LogFactory)
   '(java.net URI)))

(import-hadoop)

(declare *reporter*)

(defn counter
  ([counter-name]
     (.incrCounter *reporter* "Custom counters" (str counter-name) 1))
  ([group counter-name]
     (.incrCounter *reporter* (str group) (str counter-name) 1)))

(defn default-jobconf [this]
  (let [conf (.getConf this)
        job (JobConf. conf (.getClass this))]
    (FileInputFormat/setInputPaths job "/input")
    (FileOutputFormat/setOutputPath job (Path. "/output"))
    (.setMapperClass job IdentityMapper)
    (.setReducerClass job IdentityReducer)
    (.setInputFormat job SequenceFileInputFormat)
    (.setOutputFormat job SequenceFileOutputFormat)
    (SequenceFileOutputFormat/setOutputCompressionType job SequenceFile$CompressionType/BLOCK)
    (.setOutputKeyClass job Text)
    (.setOutputValueClass job Text)
    job))

(defmacro setup-mapreduce []
  (let [the-name (str (name (ns-name *ns*)))]
    `(do
       (import-hadoop)

       (gen-class
        :name ~the-name
        :extends org.apache.hadoop.conf.Configured
        :implements [org.apache.hadoop.util.Tool]
        :prefix "tool-"
        :main true)

       (gen-class
        :name ~(str the-name "_mapper")
        :extends org.apache.hadoop.mapred.MapReduceBase
        :implements [org.apache.hadoop.mapred.Mapper]
        :prefix "mapper-")

       (gen-class
        :name ~(str the-name "_reducer")
        :extends org.apache.hadoop.mapred.MapReduceBase
        :implements [org.apache.hadoop.mapred.Reducer]
        :prefix "reducer-")
       
       (defn ~'tool-main [& args#]
         (System/exit
          (org.apache.hadoop.util.ToolRunner/run 
           (new org.apache.hadoop.conf.Configuration)
           (. (Class/forName ~the-name) ~'newInstance)
           (into-array String args#)))))))

(def *job-path-root* "/v4")

(defn job-path
  "For a stage and (optional) corpus name (both keywords), returns 
  String of path in which Hadoop data files should be stored."
  ([stage]
     (str *job-path-root* "/" (name stage)))
  ([stage corpus]
     (str *job-path-root* "/" (name stage) "/" (name corpus))))

(defn docid-path
  "For a stage and corpus name (both keywords), returns the String of
  path in which the Docid cache file should be stored."
  [stage corpus]
  (str (job-path stage corpus) "/docids.tsv.gz"))

(defn standard-map [map-fn this wkey wvalue output reporter]
  (let [key (read-string (str wkey))
        value (read-string (str wvalue))]
    (log/debug "Mapper input: " (log/logstr key)
               " => " (log/logstr value))
    (binding [*reporter* reporter]
      (doseq [[key value] (map-fn key value)]
        (log/debug "Mapper OUTPUT: " (log/logstr key)
                   " => " (log/logstr value))
        (.collect output (Text. (pr-str key))
                  (Text. (pr-str value)))))))

(defn json-map [map-fn this wkey wvalue output reporter]
  (let [key (str wkey)
        value (walk/keywordize-keys (json/read-json-string (str wvalue)))]
    (log/debug "Mapper input: " (pr-str key)
               " => " (log/logstr value))
    (binding [*reporter* reporter]
      (doseq [[key value] (map-fn key value)]
        (log/debug "Mapper OUTPUT: " (pr-str key)
                   " => " (log/logstr value))
        (.collect output (Text. (pr-str key))
                  (Text. (pr-str value)))))))

(defn byteswritable-map [map-fn self #^Text wkey #^BytesWritable wvalue
                         #^OutputCollector output reporter]
  (let [key (str wkey)
        value (.get wvalue)
        size (.getSize wvalue)]
    (log/debug "Map input: " key " => <" size " bytes>")
    (binding [*reporter* reporter]
      (doseq [[key value] (map-fn key value size)]
        (log/debug "Map OUTPUT: " key " => "(log/logstr value))
        (.collect output (Text. (pr-str key))
                  (Text. (pr-str value)))))))

(defn standard-reduce [reduce-fn this wkey wvalues-iter output reporter]
  (let [key (read-string (str wkey))
        values (map #(read-string (str %)) (iterator-seq wvalues-iter))]
    (log/debug "Reducer input: " (log/logstr key)
               " => " (log/logstr values))
    (binding [*reporter* reporter]
      (doseq [[key value] (reduce-fn key values)]
        (log/debug "Reducer OUTPUT: " (log/logstr key)
                   " => " (log/logstr value))
        (.collect output (Text. (pr-str key))
                  (Text. (pr-str value)))))))


