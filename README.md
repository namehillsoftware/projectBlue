# project blue

![project blue](design/clearstream_logo.png)

project blue is an alternative, open source streaming music client for [JRiver Media Center](http://jriver.com/).

It's features include:

- Reliable streaming from your home server running JRiver Media Center
- Caching of audio files during playback
- Synchronize audio from a JRMC server to device
- Play local files when present and metadata match
- Updates server with now playing information
- Interfaces with Bluetooth remote control clients and Pebble watches
- Interfaces with apps that implement the Scrobble Droid interface (Simple Last.fm Scrobbler is my scrobbler of choice) for scrobbling to Last.fm or Libre.fm
- Intuitive layout

Download on the [Google Play Store](https://play.google.com/store/apps/details?id=com.lasthopesoftware.bluewater)

*Requires [JRiver Media Center](http://jriver.com/) running on your home server*

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
./build-bundle.sh
```

## Deployment

Once changes are merged, a build will be deployed and published by the project blue maintainer.

## Built With

- [Gradle](https://gradle.org/) - Dependency and Build Management
- [Android Studio and SDK](https://developer.android.com/studio/) - Tools and standard library for Android
- [Kotlin](https://kotlinlang.org/) - Language project blue is migrating to
- [Java](https://www.java.com/en/) - Majority of the project is written in Java
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

## Authors

- David Vedvick

## License

This project is licensed under the GNU Lesser General Public License v3.0 - see the [LICENSE](LICENSE) 
file for details.

## Acknowledgments

- [JRiver Media Center](https://jriver.com/)
- [ExoPlayer](https://github.com/google/ExoPlayer)
- [Lightweight Stream API](https://github.com/aNNiMON/Lightweight-Stream-API)
- [RxJava](https://github.com/ReactiveX/RxJava)
- [OkHttp](https://square.github.io/okhttp/)
- [Okio](https://github.com/square/okio)
- [Joda-Time](https://www.joda.org/joda-time/)
- [SLF4J](http://www.slf4j.org/)
- [JUnt](https://junit.org/)
- [Mockito](https://site.mockito.org/)
- [AssertJ](https://assertj.github.io/doc/)
- [Robolectric](http://robolectric.org/)
