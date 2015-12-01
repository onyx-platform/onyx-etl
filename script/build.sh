#!/bin/bash
set -e
lein clean
lein uberjar
docker build -t onyx-etl:0.1.0 .
