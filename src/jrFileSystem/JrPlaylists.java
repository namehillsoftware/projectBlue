package jrFileSystem;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import jrAccess.JrPlaylistResponse;
import jrAccess.JrSession;
import jrFileSystem.IJrDataTask.OnCompleteListener;
import jrFileSystem.IJrDataTask.OnConnectListener;
import jrFileSystem.IJrDataTask.OnErrorListener;
import jrFileSystem.IJrDataTask.OnStartListener;

public class JrPlaylists extends JrItemAsyncBase<JrPlaylist> implements IJrItem<JrPlaylist> {
	private ArrayList<JrPlaylist> mSubItems;
	private ArrayList<OnStartListener> mItemStartListeners = new ArrayList<IJrDataTask.OnStartListener>(1);
	private ArrayList<OnErrorListener> mItemErrorListeners = new ArrayList<IJrDataTask.OnErrorListener>(1);
	private ArrayList<IJrDataTask.OnCompleteListener<List<JrPlaylist>>> mOnCompleteListeners;
	
	private OnConnectListener<List<JrPlaylist>> mOnConnectListener = new OnConnectListener<List<JrPlaylist>>() {
		
		@Override
		public List<JrPlaylist> onConnect(InputStream is) {
			return JrPlaylistResponse.GetItems(is);
		}
	};
	
	private OnCompleteListener<List<JrPlaylist>> mOnCompleteListener = new OnCompleteListener<List<JrPlaylist>>() {
		
		@Override
		public void onComplete(List<JrPlaylist> result) {
			mSubItems = (ArrayList<JrPlaylist>) result;			
		}
	};
	
	public JrPlaylists(int key) {
		setKey(key);
		setValue("Playlist");
	}
	
	@Override
	public ArrayList<JrPlaylist> getSubItems() {
		if (mSubItems != null) return mSubItems;
		
		mSubItems = new ArrayList<JrPlaylist>();
		if (JrSession.accessDao == null) return mSubItems;
		try {
			mSubItems = (ArrayList<JrPlaylist>) getNewSubItemsTask().execute( "Playlists/List" ).get();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return mSubItems;
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
