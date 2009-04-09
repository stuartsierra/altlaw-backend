#!/bin/sh

source `dirname $0`/../env.sh

GENHTML_DIR=/mnt/genhtml

# Copy genhtml files
mkdir -p $GENHTML_DIR
cd $HADOOP_HOME
bin/start-dfs.sh
bin/hadoop fs -get "s3://s3dfs.altlaw.org/v4/genhtml/part-*" $GENHTML_DIR/
bin/stop-dfs.sh


