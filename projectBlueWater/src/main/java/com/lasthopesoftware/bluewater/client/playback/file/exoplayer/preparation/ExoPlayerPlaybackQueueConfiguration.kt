package com.lasthopesoftware.bluewater.client.playback.file.exoplayer.preparation

import com.lasthopesoftware.bluewater.client.playback.engine.preparation.ConfigurePreparedPlaybackQueue

object ExoPlayerPlaybackQueueConfiguration : ConfigurePreparedPlaybackQueue {
	override val maxQueueSize = 1
}
