#!/usr/bin/env bash

docker-compose run -v "$(pwd)":/src -w /src gradle \
  :projectBlueWater:testReleaseUnitTest \
  :projectBlueWater:bundleRelease
