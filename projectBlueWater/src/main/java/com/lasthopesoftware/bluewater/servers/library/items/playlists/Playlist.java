package com.lasthopesoftware.bluewater.servers.library.items.playlists;

import android.util.SparseArray;

import com.lasthopesoftware.bluewater.servers.connection.ConnectionProvider;
import com.lasthopesoftware.bluewater.servers.library.items.IItem;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.Files;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.IFilesContainer;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.IItemFiles;
import com.lasthopesoftware.bluewater.shared.AbstractIntKeyStringValue;
import com.lasthopesoftware.threading.IDataTask.OnCompleteListener;

import java.util.ArrayList;
import java.util.List;

public class Playlist extends AbstractIntKeyStringValue implements IItem, IFilesContainer {
	private final ConnectionProvider connectionProvider;

	private SparseArray<Playlist> mSubItems;
	private Playlist mParent = null;
	private String mPath;
	private String mGroup;
	private Files mFiles;
	
	private ArrayList<OnCompleteListener<List<Playlist>>> mOnCompleteListeners;

	public Playlist(ConnectionProvider connectionProvider) {
		super();
		this.connectionProvider = connectionProvider;
	}
	
	public Playlist(ConnectionProvider connectionProvider, int key) {
		this.connectionProvider = connectionProvider;
		setKey(key);
	}
	
	public Playlist(ConnectionProvider connectionProvider, int key, Playlist parent) {
		this.connectionProvider = connectionProvider;
		setKey(key);
		mParent = parent;
	}

	public void setParent(Playlist parent) {
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

	public String[] getSubItemParams() {
		return new String[] { "Playlist/Files", "Playlist=" + String.valueOf(this.getKey()) };
	}

	@Override
	public IItemFiles getFiles() {
		if (mFiles == null) mFiles = new Files(connectionProvider, getSubItemParams());
		return mFiles;
	}

	@Override
	public int compareTo(IItem another) {
		int result = this.getValue().compareTo(another.getValue());
		if (result == 0) result = this.getKey() - another.getKey();
		return result;
	}
}
