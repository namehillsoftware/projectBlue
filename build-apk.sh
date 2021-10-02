#!/usr/bin/env bash

# Generate a random ID for the build, to be used for finding the build in the docker host.
# This magic is taken from this stack overflow answer - https://stackoverflow.com/a/34329799/1189542.
BUILD_ID="$(od  -vN "8" -An -tx1  /dev/urandom | tr -d " \n")"

echo "Build ID: ${BUILD_ID}"

TAG_ID="${BUILD_ID}" docker-compose build && TAG_ID="${BUILD_ID}" docker-compose run --name "${BUILD_ID}" gradle \
  :projectBlueWater:testReleaseUnitTest \
  :projectBlueWater:assembleRelease
EXIT_CODE=${PIPESTATUS[0]}

BUILD_CONTAINER=${BUILD_ID}

docker container cp "${BUILD_CONTAINER}":/src/projectBlueWater/build ./_artifacts
docker container rm "${BUILD_CONTAINER}" -v
docker image rm "${BUILD_ID}"
docker image rm "${BUILD_ID}"

exit "${EXIT_CODE}"
