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

CLASSPATH="$ALTLAW_BACKEND/src:$ALTLAW_BACKEND/build/classes:$ALTLAW_BACKEND/build/clj_classes:$ALTLAW_BACKEND/lib/*"

JAVA_OPTS="$JAVA_OPTS -XX:+UseConcMarkSweepGC -Xmx1024m"
JAVA_OPTS="$JAVA_OPTS -Dorg.altlaw.home=$HERE"
JAVA_OPTS="$JAVA_OPTS -Dorg.altlaw.env=$ALTLAW_ENV"
JAVA_OPTS="$JAVA_OPTS -Dclojure.compile.path=build/clj_classes"

JAVA="$JAVA_HOME/bin/java -cp $CLASSPATH $JAVA_OPTS"

