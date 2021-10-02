#!/usr/bin/env bash

docker-compose build && docker-compose run --rm gradle \
  :projectBlueWater:testReleaseUnitTest \
  :projectBlueWater:bundleRelease
