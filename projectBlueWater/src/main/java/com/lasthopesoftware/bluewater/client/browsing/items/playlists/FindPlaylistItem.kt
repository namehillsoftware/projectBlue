package com.lasthopesoftware.bluewater.client.browsing.items.playlists

import com.lasthopesoftware.bluewater.client.browsing.items.Item
import com.namehillsoftware.handoff.promises.Promise

interface FindPlaylistItem {
	fun promiseItem(playlist: Playlist): Promise<Item?>
}
