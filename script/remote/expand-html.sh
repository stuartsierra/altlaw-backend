#!/bin/sh

source `dirname $0`/../env.sh

PUBLIC_DIR=$ALTLAW_HOME/var/public
GENHTML_DIR=/mnt/genhtml

# Expand files
mkdir -p $PUBLIC_DIR
cd $PUBLIC_DIR
exec $JAVA org.altlaw.util.hadoop.SeqFileToFiles $GENHTML_DIR/part-*
