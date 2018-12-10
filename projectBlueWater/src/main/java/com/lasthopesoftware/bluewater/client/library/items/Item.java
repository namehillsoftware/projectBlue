package com.lasthopesoftware.bluewater.client.library.items;

import com.lasthopesoftware.bluewater.shared.AbstractIntKeyStringValue;



public class Item extends AbstractIntKeyStringValue implements IItem {

	private int playlistId;

	public Item(int key, String value) {
		super(key, value);

	}
	
	public Item(int key) {
		super();

		this.setKey(key);
	}
	
	public Item() {
		super();
	}

	public int getPlaylistId() {
		return playlistId;
	}

	public void setPlaylistId(int playlistId) {
		this.playlistId = playlistId;
	}

	@Override
    public int hashCode() {
        return getKey();
    }
}
