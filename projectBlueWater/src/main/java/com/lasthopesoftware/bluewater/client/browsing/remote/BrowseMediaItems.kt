package com.lasthopesoftware.bluewater.client.browsing.remote

import android.support.v4.media.MediaBrowserCompat
import com.lasthopesoftware.bluewater.client.browsing.items.Item
import com.namehillsoftware.handoff.promises.Promise

interface BrowseMediaItems {
	fun promiseNowPlayingItem(): Promise<MediaBrowserCompat.MediaItem?>
	fun promiseItems(item: Item): Promise<Collection<MediaBrowserCompat.MediaItem>>
	fun promiseLibraryItems(): Promise<List<MediaBrowserCompat.MediaItem>>
	fun promiseItems(query: String): Promise<Collection<MediaBrowserCompat.MediaItem>>
}
