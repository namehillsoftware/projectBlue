package com.lasthopesoftware.bluewater.client.browsing.items.playlists;

import com.lasthopesoftware.bluewater.client.browsing.items.IItem;
import com.lasthopesoftware.bluewater.shared.AbstractIntKeyStringValue;

public class Playlist extends AbstractIntKeyStringValue implements IItem {

	public Playlist() {
		super();
	}
	
	public Playlist(int key) {
		setKey(key);
	}
}
