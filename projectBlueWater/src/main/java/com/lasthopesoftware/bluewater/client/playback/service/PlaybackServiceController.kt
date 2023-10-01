package com.lasthopesoftware.bluewater.client.playback.service

import android.content.Context
import androidx.media3.common.util.UnstableApi
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.access.stringlist.FileStringListUtilities
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.resources.executors.ThreadPools
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.queued.MessageWriter
import com.namehillsoftware.handoff.promises.queued.QueuedPromise

@UnstableApi class PlaybackServiceController(private val context: Context) : ControlPlaybackService {
	override fun initialize(libraryId: LibraryId) = PlaybackService.initialize(context, libraryId)

	override fun promiseIsMarkedForPlay(libraryId: LibraryId): Promise<Boolean> =
		PlaybackService.promiseIsMarkedForPlay(context, libraryId)

	override fun play(libraryId: LibraryId) = PlaybackService.play(context, libraryId)

	override fun pause() = PlaybackService.pause(context)

	override fun next(libraryId: LibraryId) = PlaybackService.next(context, libraryId)

	override fun previous(libraryId: LibraryId) = PlaybackService.previous(context, libraryId)
	override fun seekTo(libraryId: LibraryId, position: Int) = PlaybackService.seekTo(context, libraryId, position)
	override fun moveFile(libraryId: LibraryId, dragFrom: Int, dragTo: Int) =
		PlaybackService.moveFile(context, libraryId, dragFrom, dragTo)

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

	override fun addToPlaylist(libraryId: LibraryId, serviceFile: ServiceFile) {
		PlaybackService.addFileToPlaylist(context, libraryId, serviceFile)
	}

	override fun removeFromPlaylistAtPosition(libraryId: LibraryId, position: Int) {
		PlaybackService.removeFileAtPositionFromPlaylist(context, libraryId, position)
	}

	override fun setRepeating(libraryId: LibraryId) = PlaybackService.setRepeating(context, libraryId)

	override fun setCompleting(libraryId: LibraryId) = PlaybackService.setCompleting(context, libraryId)

	override fun clearPlaylist(libraryId: LibraryId) = PlaybackService.clearPlaylist(context, libraryId)

	override fun kill() = PlaybackService.killService(context)
}
