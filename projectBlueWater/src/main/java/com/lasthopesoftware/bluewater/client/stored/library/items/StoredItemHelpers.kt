package com.lasthopesoftware.bluewater.client.stored.library.items

import com.lasthopesoftware.bluewater.client.browsing.items.IItem
import com.lasthopesoftware.bluewater.client.browsing.items.Item
import com.lasthopesoftware.bluewater.client.browsing.items.ItemId
import com.lasthopesoftware.bluewater.client.browsing.items.KeyedIdentifier
import com.lasthopesoftware.bluewater.client.browsing.items.playlists.Playlist
import com.lasthopesoftware.bluewater.client.browsing.items.playlists.PlaylistId
import com.lasthopesoftware.bluewater.client.stored.library.items.StoredItem.ItemType

object StoredItemHelpers {
    fun getListType(item: IItem): ItemType {
        return if (item is Playlist) ItemType.PLAYLIST else ItemType.ITEM
    }

	val IItem.storedItemType
		get() = when (this) {
			is Playlist -> ItemType.PLAYLIST
			is Item -> ItemType.ITEM
			else -> throw IllegalArgumentException("this")
		}

	val KeyedIdentifier.storedItemType
		get() = when (this) {
			is ItemId -> ItemType.ITEM
			is PlaylistId -> ItemType.PLAYLIST
			else -> throw IllegalArgumentException("this")
		}
}
