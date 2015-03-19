package com.lasthopesoftware.bluewater.servers.library.items.playlists;

import android.util.SparseArray;

import com.lasthopesoftware.bluewater.servers.library.items.IItem;
import com.lasthopesoftware.bluewater.servers.library.items.playlists.Playlist;
import com.lasthopesoftware.bluewater.shared.AbstractIntKeyStringValue;

import java.util.List;


public class Playlists extends AbstractIntKeyStringValue implements IItem {

	public Playlists() {
		setKey(Integer.MAX_VALUE);
		setValue("Playlist");
	}

	@Override
	public String[] getSubItemParams() {
		return new String[] { "Playlists/List" };
	}
}
