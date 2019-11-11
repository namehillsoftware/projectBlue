# Handoff, an A+ like Promise Implementation for Java

[![Build Status](https://travis-ci.com/namehillsoftware/handoff.svg?branch=master)](https://travis-ci.com/namehillsoftware/handoff) [![Coverage Status](https://coveralls.io/repos/github/namehillsoftware/handoff/badge.svg)](https://coveralls.io/github/namehillsoftware/handoff) [![Download](https://api.bintray.com/packages/dvedvick/maven/handoff/images/download.svg)](https://bintray.com/dvedvick/maven/handoff/_latestVersion)

Handoff is a simple, A+ like Promises implementation for Java. It allows easy control flow to be written for asynchronous processes in Java:

```java
public Promise<PlaybackFile> promiseFirstFile() {
    return new Promise<>(messenger -> {
        final IPositionedFileQueueProvider queueProvider = positionedFileQueueProviders.get(nowPlaying.isRepeating);
        try {
            final PreparedPlayableFileQueue preparedPlaybackQueue = preparedPlaybackQueueResourceManagement.initializePreparedPlaybackQueue(queueProvider.provideQueue(playlist, playlistPosition));
            startPlayback(preparedPlaybackQueue, filePosition)
                .firstElement() // Easily move from one asynchronous library (RxJava) to Handoff
                .subscribe(
                    playbackFile -> messenger.sendResolution(playbackFile.asPositionedFile()), // Resolve
                    messenger::sendRejection); // Reject
        } catch (Exception e) {
            messenger.sendRejection(e);
        }
    });
}
```

And the promise can then be chained as expected. The method `then` is used to continue execution immediately using the promised result:

```java
playlist.promiseFirstFile()
    .then(f -> { // Perform another action immediately with the result - this continues on the same thread the result was returned on
        // perform action
        return f; // return a new type if wanted, return null to represent Void
    });
```

For instances where another promise is needed, the method `eventually` should be used, and `excuse` should be used for catching errors, which will fall through if not caught earlier in the method chain.

```java
playlist.promiseFirstFile()
    .eventually(f -> { // Handoff the result to a method that is expected to produce a new promise
        return new Promise<>(m -> {

        });
    })
    .excuse(e -> { // Do something with an error, errors fall through from the top, like with try/catch
        return e;
    });
```

## Installation

Handoff can be installed via Gradle:

```gradle
dependencies {
    implementation 'com.namehillsoftware:handoff:0.12.0'
}
```

## Usage

### Promise Creation

Handoff makes it easy to make any asynchronous process a promise. Take for example an asynchronous OKHTTP3 call. The `Promise` class can be extended to wrap the OKHTTP3 callback interface:

```java
class PromisedResponse extends Promise<String> implements Callback {

    @Override
    public void onFailure(Request request, IOException e) {
        reject(e);
    }

    @Override
    public void onResponse(Response response) throws IOException {
        resolve(response.body().string());
    }
}
```

Or the overloaded constructor that passes a messenger in can be used:

```java
new Promise<>(m -> {
    okHttpClient.newCall(request).enqueue(new Callback() {
        @Override
        public void onFailure(Request request, IOException e) {
            m.sendRejection(e);
        }

        @Override
        public void onResponse(Response response) throws IOException {
            m.sendResolution(response.body().string());
        }
    });
});
```

When the resolution is already known, an overloaded Promise constructor is available: `new Promise<>(data);`. Anything that inherits from `Throwable` will become a rejection: `new Promise<>(error)`. A `null` result can be returned using `Promise.empty()`.

Promises can also be combined using `Promise.whenAll(Promise<Resolution> promises...)` which will resolve when all promises complete, and `Promise.whenAny(Promise<Resolution> promises...)` which will resolve when the first promise completes.

### Continuations

Continuations are where Promises really shine. Promise-style continuations allow asynchronous control flow to be much more imperative in nature. In order to continue execution on the same thread as the promise was delivered on, use the `then` convention:

```java
playlist.promiseFirstFile()
    .then(f -> { // Perform another action immediately with the result - this continues on the same thread the result was returned on
        // perform action
        return f; // return a new type if wanted, return null to represent Void
    });
```

If it is needed for a function to execute whether the promise is resolved or rejected, the overloaded method of `then` can be used:

```java
playlist.promiseFirstFile()
    .then(
        f -> { // Perform another action immediately with the result - this continues on the same thread the result was returned on
        // perform action
        return null; // return null to represent Void
    },
    error -> {
        Logger.error("An error occured!", error); // Log some error, continue on as normal
        return null;
    });
```

Handoff also nicely supports handing off to another promise. Due to type erasure in Java, an overloaded `then` method could not be used, as is done in other languages. Instead, use the `eventually` method, signifying the continuation won't immediately complete:

```java
playlist.promiseFirstFile()
    .eventually(f -> { // Handoff the result to a method that is expected to produce a new promise
        return new Promise<>(m -> {

        });
    })
```

`eventually` also supports the overloaded error method:

```java
playlist.promiseFirstFile()
    .eventually(
        f -> { // Handoff the result to a method that is expected to produce a new promise
        return new Promise<>(m -> {

        });
    },
    e -> {
        Logger.error("An error occured!", error); // Log some error, continue on as normal
        return Promise.empty();
    })
```

### Errors

Errors fall through like they would in a try/catch in synchronous, traditional Java. Errors are not strongly typed:

```java
playlist.promiseFirstFile()
    .then(f -> { // Perform another action immediately with the result - this continues on the same thread the result was returned on
        // perform action
        throw new IOException("Uh oh!"); // return null to represent Void
    })
    .then(o -> {
        // Code here won't be executed
    })
    .excuse(error -> {
        Logger.error("An error occured!", error); // Log some error, continue on as normal

        if (error instanceof IOException)
        Logger.error("It was an IO Error too!");

        return null;
    });
```

Once trapped in a method chain, that error will go away within that method chain.

#### Unhandled rejections

Unhandled rejections can sometimes be useful to know about. These can occur in situations like this:

```java
playlist.promiseFirstFile()
    .then(f -> { // Perform another action immediately with the result - this continues on the same thread the result was returned on
        // perform action
        throw new IOException("Uh oh!"); // return null to represent Void
    }); // Nothing is done with the exception
```

To handle unhandled rejections, set a listener on `Promise.Rejections.setUnhandledRejectionsReceiver(...)`.

### Guaranteed Execution

Like the finally block, Handoff has control blocks which always guarantee execution. For a continuation that `must` happen immediately, continue using `must`:

```java
final InputStream is = body.byteStream(); 
playlist.promiseFirstFile()
    .then(f -> { // Perform another action immediately with the result - this continues on the same thread the result was returned on
        // perform action
        throw new IOException("Uh oh!"); // return null to represent Void
    })
    .then(o -> {
        // Code here won't be executed
    })
    .must(() -> is.close()) // In spite of the error, this control block will execute
    .excuse(error -> {
        Logger.error("An error occured!", error); // Log some error, continue on as normal

        if (error instanceof IOException)
        Logger.error("It was an IO Error too!");

        return null;
    });
```

For a continuation that must happen eventually, use `inevitably`:

```java
final PromisedStream is = new PromisedStream<>(body.byteStream()); // theoretical "Promised stream" which closes asynchronously
playlist.promiseFirstFile()
    .then(f -> { // Perform another action immediately with the result - this continues on the same thread the result was returned on
        // perform action
        throw new IOException("Uh oh!"); // return null to represent Void
    })
    .then(o -> {
        // Code here won't be executed
    })
    .inevitably(() -> is.promiseClose())) // In spite of the error, this control block will execute
    .excuse(error -> {
        Logger.error("An error occured!", error); // Log some error, continue on as normal

        if (error instanceof IOException)
        Logger.error("It was an IO Error too!");

        return null;
    });
```
