#!/usr/bin/env bash

docker-compose build && docker-compose run gradle \
  build \
  jacocoTestReport
