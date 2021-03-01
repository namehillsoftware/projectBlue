package com.lasthopesoftware.bluewater.client.playback.file.volume.preparation

import com.lasthopesoftware.bluewater.client.playback.file.volume.ManagePlayableFileVolume
import com.namehillsoftware.handoff.promises.Promise
import org.slf4j.LoggerFactory

class MaxFileVolumeManager(private val playableFileVolume: ManagePlayableFileVolume) : ManagePlayableFileVolume {
	private var unadjustedVolume = 1f
	private var maxFileVolume = 1f
	override fun setVolume(volume: Float): Promise<Float> {
		unadjustedVolume = volume
		val adjustedVolume = maxFileVolume * volume
		logger.debug("Volume set to $volume, adjusted volume set to $adjustedVolume")
		return playableFileVolume.setVolume(adjustedVolume)
	}

	override val volume: Promise<Float>
		get() = playableFileVolume.volume

	fun setMaxFileVolume(maxFileVolume: Float) {
		logger.debug("Max file volume set to $maxFileVolume")
		this.maxFileVolume = maxFileVolume
		setVolume(unadjustedVolume)
	}

	companion object {
		private val logger = LoggerFactory.getLogger(MaxFileVolumeManager::class.java)
	}
}
