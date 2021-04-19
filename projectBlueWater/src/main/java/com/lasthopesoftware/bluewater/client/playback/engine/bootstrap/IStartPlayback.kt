package com.lasthopesoftware.bluewater.client.playback.engine.bootstrap

import com.lasthopesoftware.bluewater.client.playback.engine.IActivePlayer
import com.lasthopesoftware.bluewater.client.playback.engine.preparation.PreparedPlayableFileQueue
import org.joda.time.Duration

interface IStartPlayback {
	fun startPlayback(preparedPlaybackQueue: PreparedPlayableFileQueue, filePosition: Duration): IActivePlayer
}
