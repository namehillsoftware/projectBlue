#!/usr/bin/env bash

# Generate a random ID for the build, to be used for finding the build in the docker host.
# This magic is taken from this stack overflow answer - https://stackoverflow.com/a/34329799/1189542.
BUILD_ID="$(od  -vN "8" -An -tx1  /dev/urandom | tr -d " \n")"

docker-compose build && docker-compose run --name "${BUILD_ID}" gradle \
  test \
  jacocoTestReport
EXIT_CODE=${PIPESTATUS[0]}

BUILD_CONTAINER=${BUILD_ID}

docker container cp "${BUILD_CONTAINER}":/src/build ./_artifacts
docker container rm "${BUILD_CONTAINER}"

exit "${EXIT_CODE}"
