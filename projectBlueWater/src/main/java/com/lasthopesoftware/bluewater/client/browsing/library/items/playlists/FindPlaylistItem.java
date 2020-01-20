package com.lasthopesoftware.bluewater.client.browsing.library.items.playlists;

import com.lasthopesoftware.bluewater.client.browsing.library.items.Item;
import com.namehillsoftware.handoff.promises.Promise;

public interface FindPlaylistItem {
	Promise<Item> promiseItem(Playlist playlist);
}
