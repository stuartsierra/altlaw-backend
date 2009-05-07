(ns org.altlaw.jobs.util.hadoop.SeqFileTextToFiles
  (:gen-class)
  (:import (org.altlaw.util.hadoop LocalSetup) 
           (org.apache.hadoop.io Text SequenceFile$Reader)
           (org.apache.hadoop.fs Path)
           (org.apache.commons.io FileUtils)
           (java.io File)))

(defn exit-with-usage []
  (println "Usage: SeqFileTextToFiles sequence_files... output_dir")
  (System/exit 1))

(defn -main [& args]
  (when (zero? (count args)) (exit-with-usage))
  (let [local (LocalSetup.)
        conf (.getConf local)
        fs (.getLocalFileSystem local)
        text-name (Text.)
        text-content (Text.)
        output-dir (File. (last args))]
    (when-not (.isDirectory output-dir)
      (println "Last argument must be a directory.")
      (exit-with-usage))
    (doseq [infile (butlast args)]
      (println "Expanding sequence file " infile)
      (let [seqfile-rdr (SequenceFile$Reader. fs (Path. infile) conf)]
        (loop []
          (when (.next seqfile-rdr text-name text-content)
            (let [outfile (File. output-dir (str text-name))
                  content (str text-content)]
              (when-not (.exists outfile)
                (FileUtils/writeStringToFile outfile content "UTF-8")))
            (recur)))))))