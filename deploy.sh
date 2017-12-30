#!/usr/bin/env bash
mvn package
mvn heroku:deploy
