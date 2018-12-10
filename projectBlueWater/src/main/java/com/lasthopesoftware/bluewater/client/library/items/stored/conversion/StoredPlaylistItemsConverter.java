package com.lasthopesoftware.bluewater.client.library.items.stored.conversion;

import com.annimon.stream.Stream;
import com.lasthopesoftware.bluewater.client.library.items.playlists.FindPlaylistItem;
import com.lasthopesoftware.bluewater.client.library.items.playlists.Playlist;
import com.lasthopesoftware.bluewater.client.library.items.stored.IStoredItemAccess;
import com.lasthopesoftware.bluewater.client.library.items.stored.StoredItem;
import com.lasthopesoftware.bluewater.client.library.items.stored.StoredItemHelpers;
import com.namehillsoftware.handoff.promises.Promise;

public class StoredPlaylistItemsConverter implements ConvertStoredPlaylistsToStoredItems {
	private final FindPlaylistItem playlistItem;
	private final IStoredItemAccess storedItemAccess;

	public StoredPlaylistItemsConverter(FindPlaylistItem playlistItem, IStoredItemAccess storedItemAccess) {

		this.playlistItem = playlistItem;
		this.storedItemAccess = storedItemAccess;
	}

	@Override
	public Promise<StoredItem> promiseConvertedStoredItem(StoredItem storedItem) {
		if (storedItem.getItemType() != StoredItem.ItemType.PLAYLIST) return new Promise<>(storedItem);

		final Playlist playlist = new Playlist(storedItem.getServiceId());
		storedItemAccess.toggleSync(playlist, false);

		return playlistItem.promiseItem(playlist)
			.eventually(item -> {
				storedItemAccess.toggleSync(item, true);
				return storedItemAccess.promiseStoredItems()
					.then(storedItems -> Stream.of(storedItems)
						.filter(i -> i.getServiceId() == item.getKey() && i.getItemType() == StoredItemHelpers.getListType(item))
						.single());
			});
	}
}
