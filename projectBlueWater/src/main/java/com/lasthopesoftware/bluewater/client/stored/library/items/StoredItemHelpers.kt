package com.lasthopesoftware.bluewater.client.stored.library.items

import com.lasthopesoftware.bluewater.client.browsing.items.IItem
import com.lasthopesoftware.bluewater.client.browsing.items.ItemId
import com.lasthopesoftware.bluewater.client.browsing.items.KeyedIdentifier
import com.lasthopesoftware.bluewater.client.browsing.items.playlists.Playlist
import com.lasthopesoftware.bluewater.client.browsing.items.playlists.PlaylistId
import com.lasthopesoftware.bluewater.client.stored.library.items.StoredItem.ItemType

object StoredItemHelpers {
    fun getListType(item: IItem): ItemType {
        return if (item is Playlist) ItemType.PLAYLIST else ItemType.ITEM
    }

	val KeyedIdentifier.storedItemType
		get() = when (this) {
			is ItemId -> StoredItem.ItemType.ITEM
			is PlaylistId -> StoredItem.ItemType.PLAYLIST
			else -> throw IllegalArgumentException("this")
		}
}
