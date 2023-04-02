package com.lasthopesoftware.bluewater.client.playback.service

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.namehillsoftware.handoff.promises.Promise

interface ControlPlaybackService {
	fun promiseIsMarkedForPlay(): Promise<Boolean>

	fun play()

	fun pause()

	fun startPlaylist(fileStringList: String, position: Int = 0)

	fun startPlaylist(serviceFiles: List<ServiceFile>, position: Int = 0)

	fun shuffleAndStartPlaylist(serviceFiles: List<ServiceFile>)

	fun addToPlaylist(serviceFile: ServiceFile)

	fun setRepeating()

	fun setCompleting()

	fun kill()
}
