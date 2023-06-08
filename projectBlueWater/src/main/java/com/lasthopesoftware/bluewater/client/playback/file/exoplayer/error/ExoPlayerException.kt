package com.lasthopesoftware.bluewater.client.playback.file.exoplayer.error

import com.lasthopesoftware.bluewater.client.playback.file.error.PlaybackException
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.ExoPlayerPlaybackHandler

class ExoPlayerException : PlaybackException {
    constructor(playbackHandler: ExoPlayerPlaybackHandler) : super(playbackHandler) {}
    constructor(playbackHandler: ExoPlayerPlaybackHandler, cause: Throwable?) : super(
        playbackHandler,
        cause
    ) {
    }
}
