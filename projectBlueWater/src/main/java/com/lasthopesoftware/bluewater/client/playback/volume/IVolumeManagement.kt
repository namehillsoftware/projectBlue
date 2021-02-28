package com.lasthopesoftware.bluewater.client.playback.volume

import com.namehillsoftware.handoff.promises.Promise

interface IVolumeManagement {
	fun setVolume(volume: Float): Promise<Float>
}
