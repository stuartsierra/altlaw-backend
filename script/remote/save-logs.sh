#!/bin/bash

source `dirname $0`/../env.sh

DATE=`date +%Y%m%d%H%M%S`
LOGFILE_NAME="www.access.log.$DATE.gz"
LOGFILE_PATH="/mnt/$LOGFILE_NAME"
LOGDIR="$ALTLAW_HOME/var/log/production"
SAVELOGS_DIR="/mnt/savelogs"

# Copy logs
mkdir -p $SAVELOGS_DIR
rm -f $SAVELOGS_DIR/*
mv $LOGDIR/www.access.log.* $SAVELOGS_DIR/
cat $SAVELOGS_DIR/* | gzip -9 > $LOGFILE_PATH
aws put altlaw.org/v4/log/www/$LOGFILE_NAME $LOGFILE_PATH
rm $LOGFILE_PATH
