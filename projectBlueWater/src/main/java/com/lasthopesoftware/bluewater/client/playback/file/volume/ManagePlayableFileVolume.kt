package com.lasthopesoftware.bluewater.client.playback.file.volume

import com.namehillsoftware.handoff.promises.Promise

interface ManagePlayableFileVolume {
	fun setVolume(volume: Float): Promise<Float>
	val volume: Promise<Float>
}
