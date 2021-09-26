#!/bin/bash

CLASSPATH=lib/guava-18.0.jar
CLASSPATH=$CLASSPATH:lib/joda-time-2.8.2.jar
CLASSPATH=$CLASSPATH:lib/gson-2.8.8.jar
CLASSPATH=$CLASSPATH:lib/protobuf-java-4.0.0-rc-2.jar
CLASSPATH=$CLASSPATH:lib/protobuf-java-util-3.9.1.jar
CLASSPATH=$CLASSPATH:lib/args4j-2.32.jar
CLASSPATH=$CLASSPATH:bin

java -classpath $CLASSPATH edmtools.tools.JpiDecode $@
