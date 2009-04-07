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

if [ -z "$ALTLAW_HOME" ]
then
    ALTLAW_HOME=`dirname $0`/..
fi

CLASSPATH="$ALTLAW_HOME/src:$ALTLAW_HOME/build/classes:$ALTLAW_HOME/build/clj_classes:$ALTLAW_HOME/lib/*"

JAVA_OPTS="$JAVA_OPTS -XX:+UseConcMarkSweepGC -Xmx1024m"
JAVA_OPTS="$JAVA_OPTS -Dorg.altlaw.home=$ALTLAW_HOME"
JAVA_OPTS="$JAVA_OPTS -Dorg.altlaw.env=$ALTLAW_ENV"
JAVA_OPTS="$JAVA_OPTS -Dclojure.compile.path=build/clj_classes"

JAVA="$JAVA_HOME/bin/java -cp $CLASSPATH $JAVA_OPTS"

