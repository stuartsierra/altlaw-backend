(ns org.altlaw.load-all
  (:require 
;;    org.altlaw.jobs.distindex
;;    org.altlaw.jobs.genhtml
;;    org.altlaw.jobs.getindexes
;;    org.altlaw.jobs.graph
;;    org.altlaw.jobs.graph-map
;;    org.altlaw.jobs.link.linkincites
;;    org.altlaw.jobs.link.linkincites-mapred
;;    org.altlaw.jobs.link.linkoutcites
;;    org.altlaw.jobs.link.linkoutcites-mapred
;;    org.altlaw.jobs.link.mergedocs
;;    org.altlaw.jobs.link.mergedocs-mapred
;;    org.altlaw.jobs.procfiles.altcrawl
   org.altlaw.jobs.procfiles.ohm1
;;    org.altlaw.jobs.procfiles.ohm1-map
   org.altlaw.jobs.procfiles.profed
;;    org.altlaw.jobs.procfiles.profed-map
;;    org.altlaw.jobs.uploadpages

   org.altlaw.test.run
   org.altlaw.test.www
   org.altlaw.test.www.search

   org.altlaw.util.context
   org.altlaw.util.date
   org.altlaw.util.files
   org.altlaw.util.hadoop
   org.altlaw.util.hadoop.SeqFileToFiles
   org.altlaw.util.log
   org.altlaw.util.lucene
   org.altlaw.util.markdown
   org.altlaw.util.solr
   org.altlaw.util.s3
   org.altlaw.util.zip

   org.altlaw.www.render
   org.altlaw.www.content-pages
   org.altlaw.www.application
   org.altlaw.www.server
))
