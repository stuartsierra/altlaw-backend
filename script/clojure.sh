#!/bin/bash

source `dirname $0`/env.sh

exec $JAVA clojure.main $@
