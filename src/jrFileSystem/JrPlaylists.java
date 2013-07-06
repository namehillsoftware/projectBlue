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
	private OnStartListener mOnStartListener;
	private OnErrorListener mOnErrorListener;
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
		mOnStartListener = listener;
	}

	@Override
	public void setOnItemsErrorListener(OnErrorListener listener) {
		mOnErrorListener = listener;
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
		LinkedList<OnStartListener> listeners = new LinkedList<OnStartListener>();
		listeners.add(mOnStartListener);
		return listeners;
	}

	@Override
	protected List<OnErrorListener> getOnItemsErrorListeners() {
		LinkedList<OnErrorListener> listeners = new LinkedList<IJrDataTask.OnErrorListener>();
		listeners.add(mOnErrorListener);
		return listeners;
	}
}
