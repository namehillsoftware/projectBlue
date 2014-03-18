package com.lasthopesoftware.bluewater.data.service.objects;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import android.util.SparseArray;

import com.j256.ormlite.logger.LoggerFactory;
import com.lasthopesoftware.bluewater.data.service.access.JrPlaylistResponse;
import com.lasthopesoftware.bluewater.data.service.access.IJrDataTask.OnCompleteListener;
import com.lasthopesoftware.bluewater.data.service.access.IJrDataTask.OnConnectListener;
import com.lasthopesoftware.bluewater.data.service.access.IJrDataTask.OnErrorListener;
import com.lasthopesoftware.bluewater.data.service.access.IJrDataTask.OnStartListener;


public class JrPlaylists extends JrItemAsyncBase<JrPlaylist> implements IJrItem<JrPlaylist> {

	private SparseArray<JrPlaylist> mMappedPlaylists;
	private ArrayList<OnStartListener<List<JrPlaylist>>> mItemStartListeners = new ArrayList<OnStartListener<List<JrPlaylist>>>(1);
	private ArrayList<OnErrorListener<List<JrPlaylist>>> mItemErrorListeners = new ArrayList<OnErrorListener<List<JrPlaylist>>>(1);
	private ArrayList<OnCompleteListener<List<JrPlaylist>>> mOnCompleteListeners;
	
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
	
	public JrPlaylists(int key) {
		setKey(key);
		setValue("Playlist");
	}
	
	public SparseArray<JrPlaylist> getMappedPlaylists() {
		if (mMappedPlaylists == null) denormalizeAndMap();
		return mMappedPlaylists;
	}
	
	private void denormalizeAndMap() {
		try {
			mMappedPlaylists = new SparseArray<JrPlaylist>(getSubItems().size());
			denormalizeAndMap(getSubItems());
		} catch (IOException io) {
			LoggerFactory.getLogger(JrPlaylists.class).error(io.getMessage(), io);
		}
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
			mOnCompleteListeners = new ArrayList<OnCompleteListener<List<JrPlaylist>>>();
		}
		if (mOnCompleteListeners.size() < 1) mOnCompleteListeners.add(listener);
		mOnCompleteListeners.set(0, listener);
	}

	@Override
	public void setOnItemsStartListener(OnStartListener<List<JrPlaylist>> listener) {
		if (mItemStartListeners.size() < 1) mItemStartListeners.add(listener); 
		mItemStartListeners.set(0, listener);
	}
	
	@Override
	public void setOnItemsErrorListener(OnErrorListener<List<JrPlaylist>> listener) {
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
	protected List<OnStartListener<List<JrPlaylist>>> getOnItemsStartListeners() {
		return mItemStartListeners;
	}

	@Override
	protected List<OnErrorListener<List<JrPlaylist>>> getOnItemsErrorListeners() {
		return mItemErrorListeners;
	}
}
