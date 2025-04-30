package com.lasthopesoftware.bluewater.client.playback.engine.bootstrap

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.NowPlaying
import com.namehillsoftware.handoff.promises.Promise

interface BootstrapPlayback {
	fun updateFromState(libraryId: LibraryId): Promise<NowPlaying?>
}
