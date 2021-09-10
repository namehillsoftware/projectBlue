package com.lasthopesoftware.bluewater.client.playback.engine.selection

import com.namehillsoftware.handoff.promises.Promise

interface SelectPlaybackEngineType {
    fun selectPlaybackEngine(playbackEngineType: PlaybackEngineType): Promise<Unit>
}
