package com.lasthopesoftware.bluewater.client.playback.engine.bootstrap

import com.lasthopesoftware.bluewater.client.playback.engine.preparation.PreparedPlayableFileQueue
import com.lasthopesoftware.bluewater.client.playback.playlist.ManagePlaylistPlayback
import org.joda.time.Duration

interface IStartPlayback {
	fun startPlayback(preparedPlaybackQueue: PreparedPlayableFileQueue, filePosition: Duration): ManagePlaylistPlayback
}
