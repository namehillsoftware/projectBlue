package com.lasthopesoftware.bluewater.client.stored.library.items

import com.lasthopesoftware.bluewater.client.browsing.items.IItem
import com.lasthopesoftware.bluewater.client.browsing.items.playlists.Playlist
import com.lasthopesoftware.bluewater.client.stored.library.items.StoredItem.ItemType

object StoredItemHelpers {
    fun getListType(item: IItem): ItemType {
        return if (item is Playlist) ItemType.PLAYLIST else ItemType.ITEM
    }
}
