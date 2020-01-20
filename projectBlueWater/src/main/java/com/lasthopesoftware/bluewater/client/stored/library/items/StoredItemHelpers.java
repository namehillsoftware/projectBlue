package com.lasthopesoftware.bluewater.client.stored.library.items;

import com.lasthopesoftware.bluewater.client.browsing.library.items.IItem;
import com.lasthopesoftware.bluewater.client.browsing.library.items.playlists.Playlist;

public class StoredItemHelpers {

	public static StoredItem.ItemType getListType(IItem item) {
		return item instanceof Playlist ? StoredItem.ItemType.PLAYLIST : StoredItem.ItemType.ITEM;
	}
}
