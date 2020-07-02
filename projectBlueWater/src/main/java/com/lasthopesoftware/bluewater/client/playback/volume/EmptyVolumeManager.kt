package com.lasthopesoftware.bluewater.client.playback.volume

import com.lasthopesoftware.bluewater.client.playback.file.volume.ManagePlayableFileVolume

class EmptyVolumeManager : ManagePlayableFileVolume {
	private var volume: Float = 0f

	override fun getVolume(): Float {
		return volume
	}

	override fun setVolume(volume: Float): Float {
		this.volume = volume
		return volume
	}
}