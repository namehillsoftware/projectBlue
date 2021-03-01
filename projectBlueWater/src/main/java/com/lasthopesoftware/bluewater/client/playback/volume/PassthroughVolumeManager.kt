package com.lasthopesoftware.bluewater.client.playback.volume

import com.lasthopesoftware.bluewater.client.playback.file.volume.ManagePlayableFileVolume
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise

class PassthroughVolumeManager : ManagePlayableFileVolume {
	private var backingVolume: Float = 0f

	override fun setVolume(volume: Float): Promise<Float> {
		this.backingVolume = volume
		return volume.toPromise()
	}

	override val volume: Promise<Float>
		get() = backingVolume.toPromise()
}
