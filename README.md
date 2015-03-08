# Clearstream _aka_ projectBlueWater
Alternative streaming music player for J River Media Center

Folder structure is organized by _context_, or rather, it moves away from the "sock drawer" folder structure
that Android follows. It is inspired by this post on AngularJS project structure (another Google framework that
encourage sock drawer app structuring): http://cliffmeyers.com/blog/2013/4/21/code-organization-angularjs-javascript.

This means everything is logically grouped in nested folders based on context. Thus, after the boilerplate folders from
Android studio, the highest level is effectively _server_:

_Under `projectBlueWater/src/main/java/com/lasthopesoftware/bluewater`:_

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
