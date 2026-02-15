#!/usr/bin/env bash

shopt -s globstar
shopt -s nullglob

# Do not set user (`-u "$(id -u)":"$(id -g)"`) because it fails during Github actions. This means that
# running this on a local machine will leave artifacts that have root ownership.

docker compose build && docker compose run --rm \
  -v "$(pwd)":/src -w /src \
#  -u "$(id -u)":"$(id -g)" \
   gradle \
  :projectBlueWater:testReleaseUnitTest \
  :projectBlueWater:assembleRelease

mkdir -p _artifacts

cp "$(pwd)"/projectBlueWater/build/**/*.apk "$(pwd)"/_artifacts/
