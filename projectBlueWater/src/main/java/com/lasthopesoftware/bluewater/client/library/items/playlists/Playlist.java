package com.lasthopesoftware.bluewater.client.library.items.playlists;

import com.lasthopesoftware.bluewater.client.library.items.IItem;
import com.lasthopesoftware.bluewater.shared.AbstractIntKeyStringValue;

public class Playlist extends AbstractIntKeyStringValue implements IItem {

	private Playlist parent = null;
	private String path;

	public Playlist() {
		super();
	}
	
	public Playlist(int key) {
		setKey(key);
	}
	
	public Playlist(int key, Playlist parent) {
		setKey(key);
		this.parent = parent;
	}

	public Playlist getParent() {
		return parent;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String mPath) {
		this.path = mPath;
	}

}
