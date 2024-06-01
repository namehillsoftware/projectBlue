package com.lasthopesoftware.bluewater.client.playback.nowplaying.view

import com.lasthopesoftware.bluewater.NavigateApplication
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise

class SkipNowPlayingNavigation(private val inner: NavigateApplication): NavigateApplication by inner {
	override fun viewNowPlaying(libraryId: LibraryId): Promise<Unit> = Unit.toPromise()
}
