package com.lasthopesoftware.bluewater.client.playback.engine

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.namehillsoftware.handoff.promises.Promise

interface ChangePlaybackState {
	fun startPlaylist(libraryId: LibraryId, playlist: List<ServiceFile>, playlistPosition: Int): Promise<Unit>
	fun resume(): Promise<Unit>
	fun pause(): Promise<Unit>
}
