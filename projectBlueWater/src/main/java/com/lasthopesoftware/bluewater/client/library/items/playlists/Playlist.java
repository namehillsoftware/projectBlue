package com.lasthopesoftware.bluewater.client.library.items.playlists;

import android.util.SparseArray;
import com.lasthopesoftware.bluewater.client.library.items.IItem;
import com.lasthopesoftware.bluewater.client.library.items.media.files.access.parameters.IFileListParameterProvider;
import com.lasthopesoftware.bluewater.shared.AbstractIntKeyStringValue;

import java.util.ArrayList;

public class Playlist extends AbstractIntKeyStringValue implements IItem, IFileListParameterProvider {

	private SparseArray<Playlist> mSubItems;
	private Playlist mParent = null;
	private String mPath;
	private String mGroup;

	public Playlist() {
		super();
	}
	
	public Playlist(int key) {
		setKey(key);
	}
	
	public Playlist(int key, Playlist parent) {
		setKey(key);
		mParent = parent;
	}

	private void setParent(Playlist parent) {
		mParent = parent;
	}
	
	public Playlist getParent() {
		return mParent;
	}
	
	public ArrayList<Playlist> getChildren() {
		if (mSubItems == null) mSubItems = new SparseArray<>();
		
		final int subItemSize = mSubItems.size();
		final ArrayList<Playlist> returnList = new ArrayList<>(subItemSize);
		for (int i = 0; i < subItemSize; i++)
			returnList.add(mSubItems.valueAt(i));
		return returnList;
	}

	public void addPlaylist(Playlist playlist) {
		if (mSubItems == null) mSubItems = new SparseArray<>();
		playlist.setParent(this);
		mSubItems.put(playlist.getKey(), playlist);
	}
	
	/**
	 * @return the mPath
	 */
	public String getPath() {
		return mPath;
	}

	/**
	 * @param mPath the mPath to set
	 */
	public void setPath(String mPath) {
		this.mPath = mPath;
	}

	/**
	 * @return the mGroup
	 */
	public String getGroup() {
		return mGroup;
	}

	/**
	 * @param mGroup the mGroup to set
	 */
	public void setGroup(String mGroup) {
		this.mGroup = mGroup;
	}

	@Override
	public String[] getFileListParameters() {
		return new String[] {"Playlist/Files", "Playlist=" + String.valueOf(getKey())};
	}
}
