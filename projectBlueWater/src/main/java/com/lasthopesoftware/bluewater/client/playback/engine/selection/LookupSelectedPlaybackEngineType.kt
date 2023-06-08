package com.lasthopesoftware.bluewater.client.playback.engine.selection

import com.namehillsoftware.handoff.promises.Promise

interface LookupSelectedPlaybackEngineType {
    fun promiseSelectedPlaybackEngineType(): Promise<PlaybackEngineType>
}
