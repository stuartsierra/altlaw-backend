#!/bin/sh

source `dirname $0`/remote-env.sh

PUBLIC_DIR=$ALTLAW_HOME/var/public

# Expand files
mkdir -p $PUBLIC_DIR
cd $PUBLIC_DIR
exec $JAVA org.altlaw.util.hadoop.SeqFileToFiles /mnt/genhtml/part-*
