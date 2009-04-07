(ns org.altlaw.util.hadoop.SeqFileToFiles
  (:require [org.altlaw.util.hadoop :as hadoop])
  (:gen-class)
  (:import (org.altlaw.util.hadoop LocalSetup)
           (org.apache.commons.io IOUtils)
           (java.io File ByteArrayInputStream FileOutputStream)
           (org.apache.hadoop.io SequenceFile$Reader)))

(hadoop/import-hadoop)

(defn -main [& args]
  (let [local (LocalSetup.)
        conf (.getConf local)
        fs (.getLocalFileSystem local)
        wname (Text.)
        wbytes (BytesWritable.)]
    (doseq [file args]
      (println "EXPANDING SEQUENCE FILE" file)
      (let [seqfile-rdr (SequenceFile$Reader. fs (Path. file) conf)]
        (loop []
          (when (.next seqfile-rdr wname wbytes)
            (let [file (File. (str wname))]
              (.mkdirs (.getParentFile file))
              (with-open [input (ByteArrayInputStream. (.get wbytes) 0
                                                       (.getSize wbytes))
                          output (FileOutputStream. file)]
                (IOUtils/copy input output)))
            (recur)))))))
