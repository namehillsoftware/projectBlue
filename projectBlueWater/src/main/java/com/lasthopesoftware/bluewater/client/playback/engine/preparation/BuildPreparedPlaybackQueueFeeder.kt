package com.lasthopesoftware.bluewater.client.playback.engine.preparation

interface BuildPreparedPlaybackQueueFeeder {
    fun build(): IPlayableFilePreparationSourceProvider?
}
