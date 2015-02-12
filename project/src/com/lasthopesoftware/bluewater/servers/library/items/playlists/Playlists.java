package com.lasthopesoftware.bluewater.servers.library.items.playlists;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import android.util.SparseArray;

import com.j256.ormlite.logger.LoggerFactory;
import com.lasthopesoftware.bluewater.data.service.access.PlaylistRequest;
import com.lasthopesoftware.bluewater.data.service.access.IDataTask.OnCompleteListener;
import com.lasthopesoftware.bluewater.data.service.access.IDataTask.OnConnectListener;
import com.lasthopesoftware.bluewater.data.service.access.IDataTask.OnErrorListener;
import com.lasthopesoftware.bluewater.data.service.access.IDataTask.OnStartListener;
import com.lasthopesoftware.bluewater.data.service.objects.IItem;
import com.lasthopesoftware.bluewater.data.service.objects.ItemAsyncBase;


public class Playlists extends ItemAsyncBase implements IItem {

	private SparseArray<Playlist> mMappedPlaylists;
	private final List<Playlist> mChildren;
//	
//	private final OnConnectListener<List<Playlist>> mOnConnectListener = new OnConnectListener<List<Playlist>>() {
//		
//		@Override
//		public List<Playlist> onConnect(InputStream is) {
//			ArrayList<Playlist> streamResult = PlaylistRequest.GetItems(is);
//			
//			int i = 0;
//			while (i < streamResult.size()) {
//				if (streamResult.get(i).getParent() != null) streamResult.remove(i);
//				else i++;
//			}
//			return streamResult;
//		}
//	};
	
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

//	@Override
//	public void addOnItemsCompleteListener(OnCompleteListener<List<Playlist>> listener) {
//		if (mOnCompleteListeners == null) mOnCompleteListeners = new ArrayList<OnCompleteListener<List<Playlist>>>();
//		
//		mOnCompleteListeners.add(listener);
//	}
//
//	@Override
//	public void removeOnItemsCompleteListener(OnCompleteListener<List<Playlist>> listener) {
//		if (mOnCompleteListeners != null)
//			mOnCompleteListeners.remove(listener);
//	}
//
//	@Override
//	protected OnConnectListener<List<Playlist>> getOnItemConnectListener() {
//		return mOnConnectListener;
//	}
//
//	@Override
//	protected List<OnCompleteListener<List<Playlist>>> getOnItemsCompleteListeners() {
//		return mOnCompleteListeners;
//	}
//
//	@Override
//	protected List<OnStartListener<List<Playlist>>> getOnItemsStartListeners() {
//		return mItemStartListeners;
//	}
//
//	@Override
//	protected List<OnErrorListener<List<Playlist>>> getOnItemsErrorListeners() {
//		return mItemErrorListeners;
//	}
}
