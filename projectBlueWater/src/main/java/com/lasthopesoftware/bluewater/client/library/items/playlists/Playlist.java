package com.lasthopesoftware.bluewater.client.library.items.playlists;

import com.lasthopesoftware.bluewater.client.library.items.IItem;
import com.lasthopesoftware.bluewater.shared.AbstractIntKeyStringValue;

public class Playlist extends AbstractIntKeyStringValue implements IItem {

	public Playlist() {
		super();
	}
	
	public Playlist(int key) {
		setKey(key);
	}
}
