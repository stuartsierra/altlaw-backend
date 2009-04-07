#!/bin/bash

if [ -z "$JAVA_HOME" ]
then
    echo "Error: JAVA_HOME not set."
    exit 1;
fi

if [ -z "$ALTLAW_ENV" ]
then
    ALTLAW_ENV="development"
fi

cd `dirname $0`
cd ..

HERE=`pwd`

CLASSPATH="$HERE/src:$HERE/build/classes:$HERE/build/clj_classes:$HERE/lib/*"

JAVA_OPTS="$JAVA_OPTS -XX:+UseConcMarkSweepGC -Xmx1024m"
JAVA_OPTS="$JAVA_OPTS -Dorg.altlaw.home=$HERE"
JAVA_OPTS="$JAVA_OPTS -Dorg.altlaw.env=$ALTLAW_ENV"
JAVA_OPTS="$JAVA_OPTS -Dclojure.compile.path=build/clj_classes"

JAVA="$JAVA_HOME/bin/java -cp $CLASSPATH $JAVA_OPTS"

