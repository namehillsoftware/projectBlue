package com.lasthopesoftware.bluewater.servers.library.items.playlists;

import com.lasthopesoftware.bluewater.servers.library.items.IItem;
import com.lasthopesoftware.bluewater.shared.AbstractIntKeyStringValue;


public class Playlists extends AbstractIntKeyStringValue implements IItem {

	public Playlists() {
		setKey(Integer.MAX_VALUE);
		setValue("Playlist");
	}

}
