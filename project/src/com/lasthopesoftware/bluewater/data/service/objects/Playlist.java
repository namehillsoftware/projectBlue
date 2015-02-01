package com.lasthopesoftware.bluewater.data.service.objects;

import java.util.ArrayList;
import java.util.List;

import android.os.AsyncTask;
import android.util.SparseArray;

import com.lasthopesoftware.bluewater.data.service.access.IDataTask.OnCompleteListener;
import com.lasthopesoftware.threading.ISimpleTask;
import com.lasthopesoftware.threading.SimpleTask;

public class Playlist extends AbstractIntKeyStringValue implements IItem<Playlist>, IFilesContainer {
	private SparseArray<Playlist> mSubItems;
	private Playlist mParent = null;
	private String mPath;
	private String mGroup;
	private Files mJrFiles;
	
	private ArrayList<OnCompleteListener<List<Playlist>>> mOnCompleteListeners;
	
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
		
		final int subItemSize = mSubItems.size();
		final ArrayList<Playlist> returnList = new ArrayList<Playlist>(subItemSize);
		for (int i = 0; i < subItemSize; i++)
			returnList.add(mSubItems.valueAt(i));
		return returnList;
	}
	
	@Override
	public void getSubItemsAsync() {
		final SimpleTask<String, Void, List<Playlist>> getPlaylistsTask = new SimpleTask<String, Void, List<Playlist>>(new ISimpleTask.OnExecuteListener<String, Void, List<Playlist>>() {

			@Override
			public List<Playlist> onExecute(ISimpleTask<String, Void, List<Playlist>> owner, String... params) throws Exception {
				return getSubItems();
			}
		});
		
		getPlaylistsTask.addOnCompleteListener(new ISimpleTask.OnCompleteListener<String, Void, List<Playlist>>() {
			
			@Override
			public void onComplete(ISimpleTask<String, Void, List<Playlist>> owner, List<Playlist> result) {
				if (mOnCompleteListeners == null) return;
				
				for (OnCompleteListener<List<Playlist>> onCompleteListener : mOnCompleteListeners)
					onCompleteListener.onComplete(owner, result);
			}
		});
		
		getPlaylistsTask.execute(AsyncTask.THREAD_POOL_EXECUTOR);
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

	@Override
	public void addOnItemsCompleteListener(OnCompleteListener<List<Playlist>> listener) {
		if (mOnCompleteListeners == null) mOnCompleteListeners = new ArrayList<OnCompleteListener<List<Playlist>>>();
		
		mOnCompleteListeners.add(listener);
	}

	@Override
	public void removeOnItemsCompleteListener(OnCompleteListener<List<Playlist>> listener) {
		if (mOnCompleteListeners != null)
			mOnCompleteListeners.remove(listener);
	}
}
