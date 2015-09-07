#!/bin/bash

CLASSPATH=lib/guava-18.0.jar
CLASSPATH=$CLASSPATH:lib/joda-time-2.8.2.jar
CLASSPATH=$CLASSPATH:lib/proto-3.0.0.jar
CLASSPATH=$CLASSPATH:lib/args4j-2.32.jar
CLASSPATH=$CLASSPATH:bin

java -classpath $CLASSPATH edmtools.tools.JpiRewrite $@
