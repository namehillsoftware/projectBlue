#!/usr/bin/env bash

docker-compose run --rm -v "$(pwd)":/src -w /src gradle \
  :projectBlueWater:testReleaseUnitTest \
  :projectBlueWater:bundleRelease
