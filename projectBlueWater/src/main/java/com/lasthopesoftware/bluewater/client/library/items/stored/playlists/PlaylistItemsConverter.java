package com.lasthopesoftware.bluewater.client.library.items.stored.playlists;

import com.lasthopesoftware.bluewater.client.library.items.playlists.Playlist;
import com.lasthopesoftware.bluewater.client.library.items.stored.StoredItem;
import com.namehillsoftware.handoff.promises.Promise;

public class PlaylistItemsConverter implements ConvertPlaylistsToItems {
	@Override
	public Promise<StoredItem> promiseStoredItem(Playlist playlist) {
		return Promise.empty();
	}
}
