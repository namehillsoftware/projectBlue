package com.lasthopesoftware.bluewater.client.library.items.playlists;

import com.lasthopesoftware.bluewater.client.library.items.Item;
import com.namehillsoftware.handoff.promises.Promise;

public interface ConvertPlaylistsToItems {
	Promise<Item> promiseItem(Playlist playlist);
}
