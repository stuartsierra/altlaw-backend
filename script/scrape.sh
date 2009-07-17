#!/bin/bash

if [ -z "$JAVA_HOME" ]
then
    JAVA=`which java`
    echo "Warning: JAVA_HOME not set; using $JAVA"
else
    JAVA="$JAVA_HOME/bin/java"
fi

cd `dirname $0`/..

CP="src"
CP="$CP:build/classes"
CP="$CP:build/clj_classes"
CP="$CP:lib/*"

exec $JAVA -cp $CP org.jruby.Main src/org/altlaw/extract/scrape/run.rb $@
