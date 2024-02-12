package com.lasthopesoftware.bluewater.client.playback.engine.bootstrap

import com.lasthopesoftware.bluewater.client.playback.engine.preparation.PreparedPlayableFileQueue
import com.lasthopesoftware.bluewater.client.playback.playlist.ManagePlaylistPlayback
import com.lasthopesoftware.bluewater.client.playback.playlist.PlaylistPlayer
import com.lasthopesoftware.bluewater.client.playback.volume.PlaylistVolumeManager
import org.joda.time.Duration
import java.io.Closeable

class PlaylistPlaybackBootstrapper(private val volumeManagement: PlaylistVolumeManager) : IStartPlayback, Closeable {

	@Volatile
	private var playlistPlayer: PlaylistPlayer? = null

	@Synchronized
	override fun startPlayback(preparedPlaybackQueue: PreparedPlayableFileQueue, filePosition: Duration): ManagePlaylistPlayback {
		playlistPlayer?.close()
		val newPlayer = PlaylistPlayer(preparedPlaybackQueue, filePosition)
		volumeManagement.managePlayer(newPlayer)
		playlistPlayer = newPlayer
		newPlayer.prepare()
		return newPlayer
	}

	override fun close() {
		playlistPlayer?.close()
	}
}
