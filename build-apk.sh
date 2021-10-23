#!/usr/bin/env bash

rm -rf _artifacts

USER="$(id -u)" GROUP="$(id -g)" docker-compose build && docker-compose run -v "$(pwd)":/src -w /src -u "$(id -u)":"$(id -g)" gradle \
  :projectBlueWater:testReleaseUnitTest \
  :projectBlueWater:assembleRelease
EXIT_CODE=${PIPESTATUS[0]}

cp -r projectBlueWater/build _artifacts

exit "${EXIT_CODE}"
