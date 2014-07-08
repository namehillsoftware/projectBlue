package com.lasthopesoftware.bluewater.data.service.objects;

import java.util.ArrayList;

import android.util.SparseArray;

public class Playlist extends BaseObject implements IItem<Playlist>, IFilesContainer {
	private SparseArray<Playlist> mSubItems;
	private Playlist mParent = null;
	private String mPath;
	private String mGroup;
	private Files mJrFiles;
	
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

	public void setParent(Playlist parent) {
		mParent = parent;
	}
	
	public Playlist getParent() {
		return mParent;
	}
	
	@Override
	public ArrayList<Playlist> getSubItems() {
		if (mSubItems == null) mSubItems = new SparseArray<Playlist>();
		ArrayList<Playlist> returnList = new ArrayList<Playlist>(mSubItems.size());
		for (int i = 0; i < mSubItems.size(); i++)
			returnList.add(mSubItems.valueAt(i));
		return returnList;
	}
	
	public void addPlaylist(Playlist playlist) {
		if (mSubItems == null) mSubItems = new SparseArray<Playlist>();
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

	protected String[] getSubItemParams() {
		return new String[] { "Playlist/Files", "Playlist=" + String.valueOf(this.getKey()) };
	}

	@Override
	public IItemFiles getFiles() {
		if (mJrFiles == null) mJrFiles = new Files("Playlist/Files", "Playlist=" + String.valueOf(this.getKey()));
		return mJrFiles;
	}

	@Override
	public int compareTo(Playlist another) {
		int result = this.getValue().compareTo(another.getValue());
		if (result == 0) result = this.getKey() - another.getKey();
		return 0;
	}
}
