package com.lasthopesoftware.bluewater.client.playback.engine.selection.defaults

import com.lasthopesoftware.bluewater.client.playback.engine.selection.PlaybackEngineType
import com.namehillsoftware.handoff.promises.Promise

object DefaultPlaybackEngineLookup : LookupDefaultPlaybackEngine {
    override fun promiseDefaultEngineType(): Promise<PlaybackEngineType> {
        return Promise(PlaybackEngineType.ExoPlayer)
    }
}
