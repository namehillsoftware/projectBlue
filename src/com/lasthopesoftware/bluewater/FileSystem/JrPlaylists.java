package com.lasthopesoftware.bluewater.FileSystem;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import com.lasthopesoftware.bluewater.FileSystem.IJrDataTask.OnCompleteListener;
import com.lasthopesoftware.bluewater.FileSystem.IJrDataTask.OnConnectListener;
import com.lasthopesoftware.bluewater.FileSystem.IJrDataTask.OnErrorListener;
import com.lasthopesoftware.bluewater.FileSystem.IJrDataTask.OnStartListener;
import com.lasthopesoftware.bluewater.access.JrPlaylistResponse;
import com.lasthopesoftware.bluewater.access.JrSession;

import android.util.Base64;
import android.util.SparseArray;


public class JrPlaylists extends JrItemAsyncBase<JrPlaylist> implements IJrItem<JrPlaylist> {
	private ArrayList<JrPlaylist> mSubItems;
	private SparseArray<JrPlaylist> mMappedPlaylists;
	private ArrayList<OnStartListener> mItemStartListeners = new ArrayList<IJrDataTask.OnStartListener>(1);
	private ArrayList<OnErrorListener> mItemErrorListeners = new ArrayList<IJrDataTask.OnErrorListener>(1);
	private ArrayList<IJrDataTask.OnCompleteListener<List<JrPlaylist>>> mOnCompleteListeners;
	
	private OnConnectListener<List<JrPlaylist>> mOnConnectListener = new OnConnectListener<List<JrPlaylist>>() {
		
		@Override
		public List<JrPlaylist> onConnect(InputStream is) {
			ArrayList<JrPlaylist> streamResult = JrPlaylistResponse.GetItems(is);
			
			int i = 0;
			while (i < streamResult.size()) {
				if (streamResult.get(i).getParent() != null) streamResult.remove(i);
				else i++;
			}
			return streamResult;
		}
	};
	
	private OnCompleteListener<List<JrPlaylist>> mOnCompleteListener = new OnCompleteListener<List<JrPlaylist>>() {
		
		@Override
		public void onComplete(List<JrPlaylist> result) {
			mSubItems = new ArrayList<JrPlaylist>(result.size());
		}
		
		
	};
	
	public JrPlaylists(int key) {
		setKey(key);
		setValue("Playlist");
	}
	
	public SparseArray<JrPlaylist> getMappedPlaylists() {
		if (mMappedPlaylists == null) denormalizeAndMap();
		return mMappedPlaylists;
	}
	
	private void denormalizeAndMap() {
		mMappedPlaylists = new SparseArray<JrPlaylist>(getSubItems().size());
		denormalizeAndMap(getSubItems());
	}
	
	private void denormalizeAndMap(ArrayList<JrPlaylist> items) {
		for (JrPlaylist playlist : items) {
			mMappedPlaylists.append(playlist.getKey(), playlist);
			if (playlist.getSubItems().size() > 0) denormalizeAndMap(playlist.getSubItems());
		}
	}
	
	@Override
	protected String[] getSubItemParams() {
		return new String[] { "Playlists/List" };
	}

	@Override
	public void setOnItemsCompleteListener(OnCompleteListener<List<JrPlaylist>> listener) {
		if (mOnCompleteListeners == null) {
			mOnCompleteListeners = new ArrayList<IJrDataTask.OnCompleteListener<List<JrPlaylist>>>();
			mOnCompleteListeners.add(mOnCompleteListener);
		}
		if (mOnCompleteListeners.size() < 2) mOnCompleteListeners.add(listener);
		mOnCompleteListeners.set(1, listener);
	}

	@Override
	public void setOnItemsStartListener(OnStartListener listener) {
		if (mItemStartListeners.size() < 1) mItemStartListeners.add(listener); 
		mItemStartListeners.set(0, listener);
	}
	
	@Override
	public void setOnItemsErrorListener(OnErrorListener listener) {
		if (mItemErrorListeners.size() < 1) mItemErrorListeners.add(listener);
		mItemErrorListeners.set(0, listener);
	}

	@Override
	protected OnConnectListener<List<JrPlaylist>> getOnItemConnectListener() {
		return mOnConnectListener;
	}

	@Override
	protected List<OnCompleteListener<List<JrPlaylist>>> getOnItemsCompleteListeners() {
		return mOnCompleteListeners;
	}

	@Override
	protected List<OnStartListener> getOnItemsStartListeners() {
		return mItemStartListeners;
	}

	@Override
	protected List<OnErrorListener> getOnItemsErrorListeners() {
		return mItemErrorListeners;
	}
}
