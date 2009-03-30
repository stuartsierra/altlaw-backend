(ns org.altlaw.util.hadoop)

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
    (.setOutputKeyClass job IntWritable)
    (.setOutputValueClass job Text)
    job))

(defmacro setup-mapreduce []
  (let [the-name (str (name (ns-name *ns*)))]
    `(do
       (import-hadoop)

       (def ~'*log* (org.apache.commons.logging.LogFactory/getLog ~the-name))

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

;; (defmulti conf [job key value]
;;           (fn [job key value] key))

;; (defmethod conf :in [job key value]
;;   (FileInputFormat/setInputPaths job value))

;; (defmethod conf :out [job key value]
;;   (FileInputFormat/setOutputPaths job (Path. value)))

;; (defmethod conf :mapper [job key value]
;;   (.setMapperClass job value))

;; (defmethod conf :reducer [job key value]
;;   (.setReducerClass job value))

;; (defmethod conf :reducers [job key value]
;;   (.setNumReduceTasks job value))

;; (defmethod conf :input-format [job key value]
;;   (.setInputFormat job
;;                    (cond
;;                     (= value :seq) SequenceFileInputFormat
;;                     (= value :text) TextInputFormat
;;                     (= value :keyval) KeyValueTextInputFormat
;;                     )))
