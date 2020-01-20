package com.lasthopesoftware.bluewater.client.browsing.items.playlists;

import com.lasthopesoftware.bluewater.client.browsing.items.Item;
import com.namehillsoftware.handoff.promises.Promise;

public interface FindPlaylistItem {
	Promise<Item> promiseItem(Playlist playlist);
}
