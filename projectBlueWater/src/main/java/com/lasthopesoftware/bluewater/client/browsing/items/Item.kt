package com.lasthopesoftware.bluewater.client.browsing.items

import com.lasthopesoftware.bluewater.client.browsing.items.playlists.PlaylistId

data class Item(override var key: Int, override var value: String? = null, val playlistId: PlaylistId? = null) :  IItem {
	val itemId = ItemId(key)
}
