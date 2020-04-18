package com.lasthopesoftware.bluewater.client.stored.library.items.conversion;

import com.annimon.stream.Stream;
import com.lasthopesoftware.bluewater.client.browsing.items.playlists.FindPlaylistItem;
import com.lasthopesoftware.bluewater.client.browsing.items.playlists.Playlist;
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.ISelectedBrowserLibraryProvider;
import com.lasthopesoftware.bluewater.client.stored.library.items.IStoredItemAccess;
import com.lasthopesoftware.bluewater.client.stored.library.items.StoredItem;
import com.lasthopesoftware.bluewater.client.stored.library.items.StoredItemHelpers;
import com.namehillsoftware.handoff.promises.Promise;

public class StoredPlaylistItemsConverter implements ConvertStoredPlaylistsToStoredItems {
	private final ISelectedBrowserLibraryProvider libraryProvider;
	private final FindPlaylistItem playlistItem;
	private final IStoredItemAccess storedItemAccess;

	public StoredPlaylistItemsConverter(ISelectedBrowserLibraryProvider libraryProvider, FindPlaylistItem playlistItem, IStoredItemAccess storedItemAccess) {
		this.libraryProvider = libraryProvider;

		this.playlistItem = playlistItem;
		this.storedItemAccess = storedItemAccess;
	}

	@Override
	public Promise<StoredItem> promiseConvertedStoredItem(StoredItem storedItem) {
		if (storedItem.getItemType() != StoredItem.ItemType.PLAYLIST) return new Promise<>(storedItem);

		return this.libraryProvider.getBrowserLibrary()
			.eventually(l -> {
				final Playlist playlist = new Playlist(storedItem.getServiceId());
				storedItemAccess.toggleSync(l.getLibraryId(), playlist, false);

				return playlistItem.promiseItem(playlist)
					.eventually(item -> {
						storedItemAccess.toggleSync(l.getLibraryId(), item, true);
						return storedItemAccess.promiseStoredItems(l.getLibraryId())
							.then(storedItems -> Stream.of(storedItems)
								.filter(i -> i.getServiceId() == item.getKey() && i.getItemType() == StoredItemHelpers.getListType(item))
								.single());
					});
			});
	}
}
