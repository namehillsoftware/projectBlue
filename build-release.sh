#!/usr/bin/env bash

rm -rf _artifacts

# Run as the owner of the current directory
BUILD_USER=$(stat -c "%u" "$(pwd)")
BUILD_GROUP=$(stat -c "%g" "$(pwd)")

docker compose build && docker compose run --rm \
  -v "$(pwd)":/src -w /src \
  -u "$BUILD_USER":"$BUILD_GROUP" gradle \
  :projectBlueWater:testReleaseUnitTest \
  :projectBlueWater:assembleRelease \
  :projectBlueWater:bundleRelease
EXIT_CODE=${PIPESTATUS[0]}

cp -r projectBlueWater/build _artifacts

exit "${EXIT_CODE}"
