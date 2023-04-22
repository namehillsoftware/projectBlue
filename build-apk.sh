#!/usr/bin/env bash

rm -rf _artifacts

docker compose build && docker compose run --rm -v "$(pwd)":/src -w /src -u "$(id -u)":"$(id -g)" gradle \
  :projectBlueWater:testReleaseUnitTest \
  :projectBlueWater:assembleRelease \
  :projectBlueWater:assembleDebug
EXIT_CODE=${PIPESTATUS[0]}

cp -r projectBlueWater/build _artifacts

exit "${EXIT_CODE}"
