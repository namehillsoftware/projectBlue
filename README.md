# audiocanoe _aka_ projectBlueWater
Alternative streaming music player for [JRiver Media Center] (http://jriver.com/)

[![Build Status](https://travis-ci.org/danrien/projectBlueWater.svg?branch=master "Build Status")](https://travis-ci.org/danrien/projectBlueWater)

Folder structure is organized by _context_, or rather, it moves away from the "sock drawer" folder structure
that Android follows. It is inspired by this post on AngularJS project structure (another Google framework that
encourage sock drawer app structuring): http://cliffmeyers.com/blog/2013/4/21/code-organization-angularjs-javascript.

This means everything is logically grouped in nested folders based on context. Thus, after the boilerplate folders from
Android studio, the highest level is effectively _server_:

Under `projectBlueWater/src/main/java/com/lasthopesoftware/bluewater`:

    server
    +-- library
    |   +-- items
    |   |   +-- media
    |   |   |   +-- files
    .
    .
    .

The above will get you to code related to files, including getting files from the server, and in more nested contexts,
file playback.

Unfortunately, other areas, such as the `res` folders, do not allow such structuring. The test project will eventually reflect this folder structuring as well, especially if it grows to the same complexity as projectBlueWater.
