package com.lasthopesoftware.bluewater.client.browsing.remote

import android.support.v4.media.MediaBrowserCompat
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.namehillsoftware.handoff.promises.Promise

interface GetNowPlayingMediaItem {
	fun promiseNowPlayingItem(libraryId: LibraryId): Promise<MediaBrowserCompat.MediaItem?>
}
