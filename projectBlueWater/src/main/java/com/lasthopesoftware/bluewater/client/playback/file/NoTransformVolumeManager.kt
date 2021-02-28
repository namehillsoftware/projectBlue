package com.lasthopesoftware.bluewater.client.playback.file

import com.lasthopesoftware.bluewater.client.playback.file.volume.ManagePlayableFileVolume
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise

class NoTransformVolumeManager : ManagePlayableFileVolume {
	private var backingVolume = 0f

	override val volume: Promise<Float>
		get() = backingVolume.toPromise()

	override fun setVolume(volume: Float): Promise<Float> {
		return volume.also { this.backingVolume = it }.toPromise()
	}
}
