#!/bin/bash

if [ -z "$JAVA_HOME" ]
then
    echo "Error: JAVA_HOME not set."
    exit 1;
fi

if [ -z "$ALTLAW_HOME" ]
then
    echo "Error: ALTLAW_HOME not set."
    exit 1
fi

if [ -z "$HADOOP_HOME" ]
then
    echo "Error: HADOOP_HOME not set."
    exit 1
fi

if [ -z "$ALTLAW_ENV" ]
then
    echo "Assuming ALTLAW_ENV=development"
    ALTLAW_ENV="development"
fi


CP="$ALTLAW_HOME/src"
CP="$CP:$ALTLAW_HOME/build/classes"
CP="$CP:$ALTLAW_HOME/build/clj_classes"
CP="$CP:$ALTLAW_HOME/lib/*"

JAVA_OPTS="$JAVA_OPTS -XX:+UseConcMarkSweepGC -Xmx1024m"
JAVA_OPTS="$JAVA_OPTS -Dorg.altlaw.home=$ALTLAW_HOME"
JAVA_OPTS="$JAVA_OPTS -Dorg.altlaw.env=$ALTLAW_ENV"
JAVA_OPTS="$JAVA_OPTS -Dclojure.compile.path=$ALTLAW_HOME/build/clj_classes"e

JAVA="$JAVA_HOME/bin/java -cp $CP $JAVA_OPTS"

