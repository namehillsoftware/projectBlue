package com.lasthopesoftware.bluewater.client.playback.service

import com.namehillsoftware.handoff.promises.Promise

interface ControlPlaybackService {
	fun promiseIsMarkedForPlay(): Promise<Boolean>

	fun setRepeating()

	fun setCompleting()

	fun play()

	fun pause()
}
