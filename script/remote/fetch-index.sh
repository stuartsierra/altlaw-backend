#!/bin/sh

source `dirname $0`/remote-env.sh

# Copy Lucene index
mkdir -p $ALTLAW_HOME/var/solr/data/index
rm -f $ALTLAW_HOME/var/solr/data/index/*
cd $HADOOP_HOME
bin/start-dfs.sh
bin/hadoop fs -get "s3://s3dfs.altlaw.org/v4/merged/*" /mnt/altlaw-backend/index/data/index/
bin/stop-dfs.sh

