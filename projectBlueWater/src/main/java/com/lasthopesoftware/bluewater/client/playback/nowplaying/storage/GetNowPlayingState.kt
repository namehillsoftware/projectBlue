package com.lasthopesoftware.bluewater.client.playback.nowplaying.storage

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.namehillsoftware.handoff.promises.Promise

interface GetNowPlayingState {
	fun promiseNowPlaying(libraryId: LibraryId): Promise<NowPlaying?>
}
