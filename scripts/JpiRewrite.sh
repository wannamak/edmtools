#!/bin/bash

CLASSPATH=lib/guava-18.0.jar
CLASSPATH=$CLASSPATH:lib/proto-3.0.0.jar
CLASSPATH=$CLASSPATH:lib/args4j-2.32.jar
CLASSPATH=$CLASSPATH:bin

java \
  -Djava.util.logging.config.file=logging.properties \
  -classpath $CLASSPATH \
  edmtools.tools.JpiRewrite \
  $@
