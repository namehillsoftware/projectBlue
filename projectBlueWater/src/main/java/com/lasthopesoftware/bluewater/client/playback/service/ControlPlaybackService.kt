package com.lasthopesoftware.bluewater.client.playback.service

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.namehillsoftware.handoff.promises.Promise

interface ControlPlaybackService {

	fun initialize(libraryId: LibraryId)

	fun promiseIsMarkedForPlay(libraryId: LibraryId): Promise<Boolean>

	fun play(libraryId: LibraryId)

	fun pause()

	fun next(libraryId: LibraryId)

	fun previous(libraryId: LibraryId)

	fun seekTo(libraryId: LibraryId, position: Int)

	fun moveFile(libraryId: LibraryId, dragFrom: Int, dragTo: Int)

	fun startPlaylist(libraryId: LibraryId, fileStringList: String, position: Int = 0)

	fun startPlaylist(libraryId: LibraryId, serviceFiles: List<ServiceFile>, position: Int = 0)

	fun shuffleAndStartPlaylist(libraryId: LibraryId, serviceFiles: List<ServiceFile>)

	fun addToPlaylist(libraryId: LibraryId, serviceFile: ServiceFile)

	fun removeFromPlaylistAtPosition(libraryId: LibraryId, position: Int)

	fun setRepeating(libraryId: LibraryId)

	fun setCompleting(libraryId: LibraryId)

	fun clearPlaylist(libraryId: LibraryId)

	fun kill()
}
