package com.lasthopesoftware.bluewater.client.library.items;

import android.support.annotation.Nullable;
import com.lasthopesoftware.bluewater.client.library.items.playlists.Playlist;
import com.lasthopesoftware.bluewater.shared.AbstractIntKeyStringValue;



public class Item extends AbstractIntKeyStringValue implements IItem {

	private Integer playlistId;

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

	@Nullable public Integer getPlaylistId() {
		return playlistId;
	}

	public Item setPlaylistId(@Nullable Integer playlistId) {
		this.playlistId = playlistId;
		return this;
	}

	@Override
    public int hashCode() {
        return getKey();
    }

    @Nullable public Playlist toPlaylist() {
		return playlistId != null
			? new Playlist(playlistId)
			: null;
	}
}
