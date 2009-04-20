#!/bin/bash

exec `dirname $0`/java.sh org.apache.lucene.misc.IndexMergeTool $@
