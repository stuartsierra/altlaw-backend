#!/bin/sh

source `dirname $0`/../env.sh

DATE=`date +%Y%m%d%H%M%S`
LOGFILE="/mnt/www.access.log.$DATE.gz"
LOGDIR=$ALTLAW_HOME/var/log/production
SAVELOGS_DIR=/mnt/savelogs

# Copy logs
mkdir -p $SAVELOGS_DIR
rm -f $SAVELOGS_DIR/*
mv $LOGDIR/www.access.log.* $SAVELOGS_DIR/
cat $SAVELOGS_DIR/* | gzip -9 > $LOGFILE
aws put altlaw.org/v4/log/www/$LOGFILE $LOGFILE
rm $LOGFILE
