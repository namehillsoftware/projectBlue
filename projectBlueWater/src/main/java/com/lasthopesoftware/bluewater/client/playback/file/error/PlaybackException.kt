package com.lasthopesoftware.bluewater.client.playback.file.error

import com.lasthopesoftware.bluewater.client.playback.file.PlayableFile

open class PlaybackException @JvmOverloads constructor(
    val playbackHandler: PlayableFile,
    cause: Throwable? = null
) : Exception(cause)
