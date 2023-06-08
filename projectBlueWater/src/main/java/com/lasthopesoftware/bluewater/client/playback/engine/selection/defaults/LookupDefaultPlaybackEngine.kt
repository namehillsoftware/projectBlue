package com.lasthopesoftware.bluewater.client.playback.engine.selection.defaults

import com.lasthopesoftware.bluewater.client.playback.engine.selection.PlaybackEngineType
import com.namehillsoftware.handoff.promises.Promise

interface LookupDefaultPlaybackEngine {
    fun promiseDefaultEngineType(): Promise<PlaybackEngineType>
}
