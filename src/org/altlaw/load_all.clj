(ns org.altlaw.load-all
  (:require 
   org.altlaw.internal.application

   org.altlaw.test.internal
   org.altlaw.test.internal.idserver.impl
   org.altlaw.test.internal.idserver.client
   org.altlaw.test.internal.privacy.impl
   ;; org.altlaw.test.internal.privacy.client
   org.altlaw.test.run

   org.altlaw.util.context
   org.altlaw.util.date
   org.altlaw.util.files
   org.altlaw.util.hadoop
   org.altlaw.util.log
   org.altlaw.util.lucene
   org.altlaw.util.markdown
   org.altlaw.util.solr
   org.altlaw.util.zip

   org.altlaw.www.render
   org.altlaw.www.content-pages
))
