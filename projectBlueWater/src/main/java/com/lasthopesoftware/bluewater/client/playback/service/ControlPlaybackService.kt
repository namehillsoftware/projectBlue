package com.lasthopesoftware.bluewater.client.playback.service

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.namehillsoftware.handoff.promises.Promise

interface ControlPlaybackService {
	fun promiseIsMarkedForPlay(): Promise<Boolean>

	fun play()

	fun pause()

	fun next()

	fun previous()

	fun seekTo(position: Int)

	fun moveFile(dragFrom: Int, dragTo: Int)

	fun startPlaylist(fileStringList: String, position: Int = 0)

	fun startPlaylist(serviceFiles: List<ServiceFile>, position: Int = 0)

	fun shuffleAndStartPlaylist(serviceFiles: List<ServiceFile>)

	fun addToPlaylist(serviceFile: ServiceFile)

	fun removeFromPlaylistAtPosition(position: Int)

	fun setRepeating()

	fun setCompleting()

	fun kill()
}
