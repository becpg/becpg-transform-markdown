#!/bin/bash
mvn clean install && docker build -t "becpg-transform-markdown:1.0.0" --build-arg JAVA_OPTS="" .