#!/bin/bash

exec `dirname $0`/jruby.sh -S irb $@
