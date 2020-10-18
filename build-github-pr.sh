#!/usr/bin/env bash

docker-compose build && docker-compose run gradle \
  :projectBlueWater:testReleaseUnitTest \
  :projectBlueWater:bundleRelease
