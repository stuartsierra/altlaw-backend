#!/bin/bash

if [ -z "$JAVA_HOME" ]
then
    echo "Error: JAVA_HOME not set."
    exit 1;
fi

cd `dirname $0`
cd ..

HERE=`pwd`

CLASSPATH="src:test:build/classes:build/clj_classes:lib/*"

JAVA_OPTS="$JAVA_OPTS -XX:+UseConcMarkSweepGC -Xmx1024m"
JAVA_OPTS="$JAVA_OPTS -Dorg.altlaw.home=$HERE"
JAVA_OPTS="$JAVA_OPTS -Dclojure.compile.path=build/clj_classes"

JAVA="$JAVA_HOME/bin/java -cp $CLASSPATH $JAVA_OPTS"

