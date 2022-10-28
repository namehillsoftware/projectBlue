#!/usr/bin/env bash

# Do not set user (`-u "$(id -u)":"$(id -g)"`) because it fails during Github actions. This means that
# running this on a local machine will leave artifacts that have root ownership.

docker-compose build && docker-compose run --rm -v "$(pwd)":/src -w /src gradle \
  :projectBlueWater:testReleaseUnitTest \
  :projectBlueWater:bundleRelease
