#!/usr/bin/env bash

docker-compose run -v "$(pwd)":/src -w /src -u "$(id -u)":"$(id -g)" gradle \
  :projectBlueWater:testReleaseUnitTest \
  :projectBlueWater:bundleRelease
