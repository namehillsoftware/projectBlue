![project blue](design/feature-graphic/feature_graphic.png)

### Alternative streaming music player for [JRiver Media Center](http://jriver.com/)

Project Blue is published on the Play Store and can be found here - https://play.google.com/store/apps/details?id=com.lasthopesoftware.bluewater.

#### Contributing

Finding issues and reporting them is contributing to making project blue better! Please [submit any issues that are found](https://github.com/namehillsoftware/projectBlue/issues) under the issues area.

#### Submitting Changes

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
