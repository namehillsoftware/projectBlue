package com.lasthopesoftware.bluewater.servers.library.items.playlists;

import java.util.List;

import android.util.SparseArray;

import com.lasthopesoftware.bluewater.servers.library.items.IItem;
import com.lasthopesoftware.bluewater.shared.AbstractIntKeyStringValue;


public class Playlists extends AbstractIntKeyStringValue implements IItem {

	private SparseArray<Playlist> mMappedPlaylists;
	private final List<Playlist> mChildren;
	
	public Playlists(int key, List<Playlist> children) {
		setKey(key);
		setValue("Playlist");
		mChildren = children;
	}
		
	public SparseArray<Playlist> getMappedPlaylists() {
		if (mMappedPlaylists == null) denormalizeAndMap();
		return mMappedPlaylists;
	}
	
	private void denormalizeAndMap() {
		mMappedPlaylists = new SparseArray<Playlist>(mChildren.size());
		denormalizeAndMap(mChildren);
	}
	
	private void denormalizeAndMap(List<Playlist> items) {
		for (Playlist playlist : items) {
			mMappedPlaylists.append(playlist.getKey(), playlist);
			if (playlist.getChildren().size() > 0) denormalizeAndMap(playlist.getChildren());
		}
	}
	
	@Override
	public String[] getSubItemParams() {
		return new String[] { "Playlists/List" };
	}
}
