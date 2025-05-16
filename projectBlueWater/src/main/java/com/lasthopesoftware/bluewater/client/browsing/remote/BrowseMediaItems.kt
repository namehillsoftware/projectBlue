package com.lasthopesoftware.bluewater.client.browsing.remote

import android.support.v4.media.MediaBrowserCompat
import com.lasthopesoftware.bluewater.client.browsing.items.ItemId
import com.lasthopesoftware.bluewater.client.browsing.items.playlists.PlaylistId
import com.namehillsoftware.handoff.promises.Promise

interface BrowseMediaItems {
	fun promiseItems(itemId: ItemId): Promise<Collection<MediaBrowserCompat.MediaItem>>
	fun promiseItems(playlistId: PlaylistId): Promise<Collection<MediaBrowserCompat.MediaItem>>
	fun promiseLibraryItems(): Promise<List<MediaBrowserCompat.MediaItem>>
	fun promiseItems(query: String): Promise<Collection<MediaBrowserCompat.MediaItem>>
}
