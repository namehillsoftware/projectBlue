package com.lasthopesoftware.bluewater.client.playback.volume

import com.lasthopesoftware.bluewater.client.playback.exoplayer.PromisingExoPlayer
import com.lasthopesoftware.bluewater.client.playback.file.volume.ManagePlayableFileVolume
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise

class AudioTrackVolumeManager(private val exoPlayer: PromisingExoPlayer) : IVolumeManagement, ManagePlayableFileVolume {
	@Volatile
	private var backingVolume = 0f.toPromise()

	override fun setVolume(volume: Float): Promise<Float> {
		backingVolume = exoPlayer
			.setVolume(volume)
			.then { volume }

		return backingVolume
	}

	override val volume: Promise<Float>
		get() = backingVolume
}
