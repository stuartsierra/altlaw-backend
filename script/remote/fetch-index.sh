#!/bin/sh

source `dirname $0`/remote-env.sh

INDEX_DIR=$ALTLAW_HOME/var/solr/data/index

# Copy Lucene index
mkdir -p $INDEX_DIR
rm -f $INDEX_DIR/*
cd $HADOOP_HOME
bin/start-dfs.sh
bin/hadoop fs -get "s3://s3dfs.altlaw.org/v4/merged/*" $INDEX_DIR/
bin/stop-dfs.sh

