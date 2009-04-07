#!/bin/sh

# Copy genhtml files
mkdir -p /mnt/genhtml
cd $HADOOP_HOME
bin/start-dfs.sh
bin/hadoop fs -get "s3://s3dfs.altlaw.org/v4/genhtml/part-*" /mnt/genhtml/
bin/stop-dfs.sh


