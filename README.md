# project blue

![project blue](./design/project-blue-logo-circular.png)

### An alternative, open source streaming music client

project blue is a streaming audio client for your home media server. Stream your favorite music and audio from your server wherever you are!

Its features include:

* Reliable streaming from your home server via an intuitive layout. Supported servers:
  * [JRiver Media Center](http://jriver.com/)
  * Subsonic (alpha - tested with [Navidrome](https://www.navidrome.org/) only)
* Caching of audio files during playback.
* Synchronize audio from server to device.
* Play local files when present and metadata match.
* Updates server with playback statistics.
* Edit and update playlists through Now Playing.
* Interfaces with Bluetooth remote control clients.
* Android TV Support
* ~~Supports Android Auto (beta)~~ NOT supported due to strict Google Play requirements.
* Interfaces with apps that implement the Scrobble Droid interface (Simple Last.fm Scrobbler is my scrobbler of choice) for scrobbling to Last.fm or Libre.fm

Download the latest from the [releases page](https://github.com/namehillsoftware/projectBlue/releases/latest), or the not-as-frequently updated version on the [Google Play Store](https://play.google.com/store/apps/details?id=com.lasthopesoftware.bluewater).

# Development

![](https://github.com/actions/namehillsoftware/projectBlue/workflows/.github/workflows/build.yml/badge.svg)

## Getting Started

These instructions will get you a copy of the project up and running on your local machine for development 
and testing purposes.

### Prerequisites

Java SDK 8+ and the Android SDK are required to develop project blue. To perform a continuous integration 
(CI) build, Docker and Docker Compose are required.

### Installing

1. Clone this repository
2. Install Android Studio, along with the Android SDK
3. Build and run!

## Running the tests

Run the tests via the IDE.

## Running the Build

Android Studio can perform the build. To run a CI build like it is run in Github, run the following command:

```shell script
./build-github-pr.sh
```

This will leave artifacts that are owned by the root user. To run a build that leaves artifacts
owned by the current user, run either `./build-release.sh` or `./build-apk.sh`.

## Deployment

Once changes are merged, a build will be deployed and published by the project blue maintainer.

## Built With

- [Gradle](https://gradle.org/) - Dependency and Build Management
- [Android Studio and SDK](https://developer.android.com/studio/) - Tools and standard library for Android
- [Kotlin](https://kotlinlang.org/) - Majority of project blue is written in Kotlin
- [Handoff](https://github.com/namehillsoftware/handoff) - Continuations library

## Contributing

All code considered guilty until proven innocent! Please ensure that your code is proven with unit 
tests before submitting a PR. No PR will be considered without the tests to back it up.

All new changes should be written in Kotlin if possible.

Folder structure is organized by **feature**, or rather, it doesn't use the "sock drawer" folder structure
that Android follows. It is inspired by this post on AngularJS project structure (another Google framework that
encourage sock drawer app structuring): http://cliffmeyers.com/blog/2013/4/21/code-organization-angularjs-javascript.
This means everything is logically grouped in nested folders based on feature. Unfortunately, some 
areas, such as the `res` folders, do not allow such structuring.

Finding issues and reporting them is also contributing to making project blue better! Please submit any 
issues that are found in the issues area.

Please read [CONTRIBUTING.md](CONTRIBUTING.md) for more details on our code of conduct, and the 
process for submitting pull requests to us.

## Versioning

We use [SemVer](http://semver.org/) for versioning. For the versions available, see the 
[tags on this repository](https://github.com/namehillsoftware/projectBlue/tags).

## UI Design

project blue has three major UI "areas":

- The browser: displays items and files from the library server. Also displays local library information
  like active file synchronization status.
- Now playing: displays information about currently playing files.
- File details: displays information about a file.

The UI design is governed by a philosophy of "owners" for the above areas. These "owners" determine
the design focus that is applied to a given area of the application:

- Browser - the application (project blue).
  - Focus on browsing the server, configuring the application, and syncing files to the device.
- Now playing - the user.
  - Focus on controlling playback, viewing current playback, and manipulating the playing playlist.
- File details - the artist.
  - Focus on displaying and editing details about a given track.

## Authors

- David Vedvick

## License

This project is licensed under the GNU Lesser General Public License v3.0 - see the [LICENSE](LICENSE) 
file for details.

## Acknowledgments

***Cue public broadcasting voice***

While project blue is maintained by one person, it is only possible because of the amazing work of
hundreds of other developers. A non-exhaustive list of the work of others that project
blue depends on is viewable here [acknowledgements.md](./acknowledgements.md).
