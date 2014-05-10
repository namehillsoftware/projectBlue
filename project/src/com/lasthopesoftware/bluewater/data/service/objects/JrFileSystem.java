package com.lasthopesoftware.bluewater.data.service.objects;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeSet;

import android.os.AsyncTask;

import com.lasthopesoftware.bluewater.data.service.access.IJrDataTask.OnCompleteListener;
import com.lasthopesoftware.bluewater.data.service.access.IJrDataTask.OnConnectListener;
import com.lasthopesoftware.bluewater.data.service.access.IJrDataTask.OnErrorListener;
import com.lasthopesoftware.bluewater.data.service.access.IJrDataTask.OnStartListener;
import com.lasthopesoftware.bluewater.data.service.access.JrFsResponse;
import com.lasthopesoftware.bluewater.data.session.JrSession;
import com.lasthopesoftware.threading.ISimpleTask;
import com.lasthopesoftware.threading.ISimpleTask.OnExecuteListener;
import com.lasthopesoftware.threading.SimpleTask;

public class JrFileSystem extends JrItemAsyncBase<IJrItem<?>> implements IJrItem<IJrItem<?>> {
	private TreeSet<IJrItem<?>> mVisibleViews;
	private JrPlaylists mPlaylistsView;
	private int[] mVisibleViewKeys;
	
	private OnCompleteListener<List<IJrItem<?>>> mOnCompleteClientListener;
	private OnStartListener<List<IJrItem<?>>> mOnStartListener;
	private OnConnectListener<List<IJrItem<?>>> mOnConnectListener;
	private OnErrorListener<List<IJrItem<?>>> mOnErrorListener;
	
	private static Object syncObject = new Object();
	
	public JrFileSystem(int... visibleViewKeys) {
		super();
		
		mVisibleViewKeys = visibleViewKeys;
		
		mOnConnectListener = new OnConnectListener<List<IJrItem<?>>>() {
			
			@Override
			public List<IJrItem<?>> onConnect(InputStream is) {
				ArrayList<IJrItem<?>> returnList = new ArrayList<IJrItem<?>>();
				for (JrItem item : JrFsResponse.GetItems(is))
					returnList.add(item);
				
				return returnList;
			}
		};
		
	}
	
	public String getSubItemUrl() {
		return JrSession.accessDao.getJrUrl("Browse/Children");
	}
	
	public void setVisibleViews(int... visibleViewKeys) {
		if (Arrays.equals(mVisibleViewKeys, visibleViewKeys)) return;
		mVisibleViewKeys = visibleViewKeys;
		synchronized(syncObject) {
			mVisibleViews = null;
		}
	}
	
	public ArrayList<IJrItem<?>> getVisibleViews() {
		try {
			return getVisibleViewsTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR).get();
		} catch (Exception e) {
			return new ArrayList<IJrItem<?>>();
		}
	}
	
	public void getVisibleViewsAsync() {
		getVisibleViewsAsync(null);
	}
	
	public void getVisibleViewsAsync(ISimpleTask.OnCompleteListener<String, Void, ArrayList<IJrItem<?>>> onCompleteListener) {
		SimpleTask<String, Void, ArrayList<IJrItem<?>>> getViewsTask = getVisibleViewsTask();
		
		if (onCompleteListener != null) getViewsTask.addOnCompleteListener(onCompleteListener);
		
		getViewsTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}
	
	private SimpleTask<String, Void, ArrayList<IJrItem<?>>> getVisibleViewsTask() {
		SimpleTask<String, Void, ArrayList<IJrItem<?>>> getViewsTask = new SimpleTask<String, Void, ArrayList<IJrItem<?>>>();
				
		getViewsTask.setOnExecuteListener(new OnExecuteListener<String, Void, ArrayList<IJrItem<?>>>() {
			
			@Override
			public ArrayList<IJrItem<?>> onExecute(ISimpleTask<String, Void, ArrayList<IJrItem<?>>> owner, String... params) throws Exception {
				synchronized(syncObject) {
					if (mVisibleViews == null || mVisibleViews.size() == 0) {
						List<IJrItem<?>> libraries = getSubItems();
						mVisibleViews = new TreeSet<IJrItem<?>>(new Comparator<IJrItem<?>>() {
	
							@Override
							public int compare(IJrItem<?> lhs, IJrItem<?> rhs) {
								return lhs.getKey() - rhs.getKey();
							}
						});
						
						for (int viewKey : mVisibleViewKeys) {
							for (IJrItem<?> library : libraries) {
								if (mVisibleViewKeys.length > 0 && viewKey != library.getKey()) continue;
								
								if (library.getValue().equalsIgnoreCase("Playlists")) {
									if (mPlaylistsView == null) mPlaylistsView = new JrPlaylists(Integer.MAX_VALUE);
									mVisibleViews.add(new JrPlaylists(Integer.MAX_VALUE));
									continue;
								}
								
								for (IJrItem<?> view : library.getSubItems())
									mVisibleViews.add(view);
							}
						}
					}
				}
				
				return new ArrayList<IJrItem<?>>(mVisibleViews);
			}
		});
		
		return getViewsTask;
	}

	@Override
	public void setOnItemsCompleteListener(OnCompleteListener<List<IJrItem<?>>> listener) {
		mOnCompleteClientListener = listener;
	}

	@Override
	public void setOnItemsStartListener(OnStartListener<List<IJrItem<?>>> listener) {
		mOnStartListener = listener;
	}

	@Override
	public void setOnItemsErrorListener(OnErrorListener<List<IJrItem<?>>> listener) {
		mOnErrorListener = listener;
	}

	@Override
	protected OnConnectListener<List<IJrItem<?>>> getOnItemConnectListener() {
		return mOnConnectListener;
	}

	@Override
	protected List<OnCompleteListener<List<IJrItem<?>>>> getOnItemsCompleteListeners() {
		LinkedList<OnCompleteListener<List<IJrItem<?>>>> listeners = new LinkedList<OnCompleteListener<List<IJrItem<?>>>>();
		if (mOnCompleteClientListener != null) listeners.add(mOnCompleteClientListener);
		return listeners;
	}

	@Override
	protected List<OnStartListener<List<IJrItem<?>>>> getOnItemsStartListeners() {
		LinkedList<OnStartListener<List<IJrItem<?>>>> listeners = new LinkedList<OnStartListener<List<IJrItem<?>>>>();
		if (mOnStartListener != null) listeners.add(mOnStartListener);
		return listeners;
	}

	@Override
	protected List<OnErrorListener<List<IJrItem<?>>>> getOnItemsErrorListeners() {
		LinkedList<OnErrorListener<List<IJrItem<?>>>> listeners = new LinkedList<OnErrorListener<List<IJrItem<?>>>>();
		if (mOnErrorListener != null) listeners.add(mOnErrorListener);
		return listeners;
	}

	@Override
	public String[] getSubItemParams() {
		return new String[] { "Browse/Children" };
	}

	@Override
	public int compareTo(IJrItem<?> another) {
		return 0;
	}
}

