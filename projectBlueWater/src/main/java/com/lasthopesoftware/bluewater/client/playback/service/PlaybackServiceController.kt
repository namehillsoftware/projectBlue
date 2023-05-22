package com.lasthopesoftware.bluewater.client.playback.service

import android.content.Context
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.access.stringlist.FileStringListUtilities
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.resources.executors.ThreadPools
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.queued.MessageWriter
import com.namehillsoftware.handoff.promises.queued.QueuedPromise

class PlaybackServiceController(private val context: Context) : ControlPlaybackService {
	override fun promiseIsMarkedForPlay(libraryId: LibraryId): Promise<Boolean> =
		PlaybackService.promiseIsMarkedForPlay(context, libraryId)

	override fun play() = PlaybackService.play(context)

	override fun pause() = PlaybackService.pause(context)

	override fun next() = PlaybackService.next(context)

	override fun previous() = PlaybackService.previous(context)
	override fun seekTo(position: Int) = PlaybackService.seekTo(context, position)
	override fun moveFile(dragFrom: Int, dragTo: Int) = PlaybackService.moveFile(context, dragFrom, dragTo)

	override fun startPlaylist(libraryId: LibraryId, fileStringList: String, position: Int) {
		PlaybackService.launchMusicService(context, libraryId, position, fileStringList)
	}

	override fun startPlaylist(libraryId: LibraryId, serviceFiles: List<ServiceFile>, position: Int) {
		FileStringListUtilities
			.promiseSerializedFileStringList(serviceFiles)
			.then { startPlaylist(libraryId, it, position) }
	}

	override fun shuffleAndStartPlaylist(libraryId: LibraryId, serviceFiles: List<ServiceFile>) {
		QueuedPromise(MessageWriter {
			startPlaylist(libraryId, serviceFiles.shuffled())
		}, ThreadPools.compute)
	}

	override fun addToPlaylist(serviceFile: ServiceFile) {
		PlaybackService.addFileToPlaylist(context, serviceFile.key)
	}

	override fun removeFromPlaylistAtPosition(position: Int) {
		PlaybackService.removeFileAtPositionFromPlaylist(context, position)
	}

	override fun setRepeating() = PlaybackService.setRepeating(context)

	override fun setCompleting() = PlaybackService.setCompleting(context)
	override fun kill() = PlaybackService.killService(context)
}
