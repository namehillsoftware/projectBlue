package com.lasthopesoftware.bluewater.client.playback.volume

import com.lasthopesoftware.bluewater.client.playback.playlist.IPlaylistPlayer
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise

class PlaylistVolumeManager(private var volume: Float) : IVolumeManagement {
	private var playlistPlayer: IPlaylistPlayer? = null

	fun managePlayer(playlistPlayer: IPlaylistPlayer?) {
		this.playlistPlayer = playlistPlayer
		playlistPlayer?.setVolume(volume)
	}

	override fun setVolume(volume: Float): Promise<Float> {
		this.volume = volume

		return playlistPlayer?.setVolume(this.volume)?.then { this.volume } ?: this.volume.toPromise()
	}
}
