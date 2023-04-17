package com.lasthopesoftware.bluewater.client.playback.service

import android.content.Context
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.access.stringlist.FileStringListUtilities
import com.lasthopesoftware.resources.executors.ThreadPools
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.queued.MessageWriter
import com.namehillsoftware.handoff.promises.queued.QueuedPromise

class PlaybackServiceController(private val context: Context) : ControlPlaybackService {
	override fun promiseIsMarkedForPlay(): Promise<Boolean> = PlaybackService.promiseIsMarkedForPlay(context)

	override fun play() = PlaybackService.play(context)

	override fun pause() = PlaybackService.pause(context)

	override fun next() = PlaybackService.next(context)

	override fun previous() = PlaybackService.previous(context)

	override fun startPlaylist(fileStringList: String, position: Int) {
		PlaybackService.launchMusicService(context, position, fileStringList)
	}

	override fun startPlaylist(serviceFiles: List<ServiceFile>, position: Int) {
		FileStringListUtilities
			.promiseSerializedFileStringList(serviceFiles)
			.then { startPlaylist(it, position) }
	}

	override fun shuffleAndStartPlaylist(serviceFiles: List<ServiceFile>) {
		QueuedPromise(MessageWriter {
			startPlaylist(serviceFiles.shuffled())
		}, ThreadPools.compute)
	}

	override fun addToPlaylist(serviceFile: ServiceFile) {
		PlaybackService.addFileToPlaylist(context, serviceFile.key)
	}

	override fun setRepeating() = PlaybackService.setRepeating(context)

	override fun setCompleting() = PlaybackService.setCompleting(context)
	override fun kill() = PlaybackService.killService(context)
}
