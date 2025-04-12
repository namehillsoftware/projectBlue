package com.lasthopesoftware.bluewater.client.playback.nowplaying

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.NowPlaying

fun singleNowPlaying(libraryId: LibraryId, serviceFile: ServiceFile) : NowPlaying =
	NowPlaying(
		libraryId,
		listOf(serviceFile),
		0,
		0L,
		false,
	)
