#!/bin/sh

# Copy genhtml files
mkdir -p /mnt/genhtml
cd /mnt/hadoop
bin/start-dfs.sh
bin/hadoop fs -get "s3://s3dfs.altlaw.org/v4/genhtml/part-*" /mnt/genhtml/
bin/stop-dfs.sh

# Expand files
cd /mnt/altlaw-backend/var/public
java -cp "../job.jar:../lib/*" org.altlaw.hadoop.SeqFileToFiles /mnt/genhtml/part-*

