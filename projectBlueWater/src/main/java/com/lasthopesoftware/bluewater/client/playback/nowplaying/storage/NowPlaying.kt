package com.lasthopesoftware.bluewater.client.playback.nowplaying.storage

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.playback.file.PositionedFile

data class NowPlaying(
	val libraryId: LibraryId,
	val playlist: List<ServiceFile>,
    val playlistPosition: Int,
    val filePosition: Long,
    val isRepeating: Boolean
) {

	val playingFile: PositionedFile?
		get() =
			playlistPosition.takeIf { it > -1 && it < playlist.size }?.let { PositionedFile(it, playlist[it]) }
}
