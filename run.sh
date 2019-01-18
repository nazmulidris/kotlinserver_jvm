#!/usr/bin/env bash
mvn package
mvn exec:java -f pom.xml
