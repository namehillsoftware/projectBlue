package com.lasthopesoftware.bluewater.data.service.objects;

import java.util.ArrayList;
import java.util.HashMap;

import android.annotation.SuppressLint;

@SuppressLint("UseSparseArrays")
public class JrPlaylist extends JrObject implements IJrItem<JrPlaylist>, IJrFilesContainer {
	private HashMap<Integer, JrPlaylist> mSubItems;
	private JrPlaylist mParent = null;
	private String mPath;
	private String mGroup;
	private JrFiles mJrFiles;
	
	public JrPlaylist() {
		super();
	}
	
	public JrPlaylist(int key) {
		setKey(key);
	}
	
	public JrPlaylist(int key, JrPlaylist parent) {
		setKey(key);
		mParent = parent;
	}

	public void setParent(JrPlaylist parent) {
		mParent = parent;
	}
	
	public JrPlaylist getParent() {
		return mParent;
	}
	
	@Override
	public ArrayList<JrPlaylist> getSubItems() {
		if (mSubItems == null) mSubItems = new HashMap<Integer, JrPlaylist>();
		return new ArrayList<JrPlaylist>(mSubItems.values());
	}
	
	public void addPlaylist(JrPlaylist playlist) {
		if (mSubItems == null) mSubItems = new HashMap<Integer, JrPlaylist>();
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
	public IJrItemFiles getJrFiles() {
		if (mJrFiles == null) mJrFiles = new JrFiles("Playlist/Files", "Playlist=" + String.valueOf(this.getKey()));
		return mJrFiles;
	}

	@Override
	public int compareTo(JrPlaylist another) {
		int result = this.getValue().compareTo(another.getValue());
		if (result == 0) result = this.getKey().compareTo(another.getKey());
		return 0;
	}
}
