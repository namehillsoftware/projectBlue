package com.lasthopesoftware.bluewater.data.service.objects;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeSet;

import android.content.Context;
import android.os.AsyncTask;

import com.lasthopesoftware.bluewater.data.service.access.IDataTask.OnCompleteListener;
import com.lasthopesoftware.bluewater.data.service.access.IDataTask.OnConnectListener;
import com.lasthopesoftware.bluewater.data.service.access.IDataTask.OnErrorListener;
import com.lasthopesoftware.bluewater.data.service.access.IDataTask.OnStartListener;
import com.lasthopesoftware.bluewater.data.service.access.FilesystemResponse;
import com.lasthopesoftware.bluewater.data.service.access.connection.ConnectionManager;
import com.lasthopesoftware.bluewater.data.sqlite.access.LibrarySession;
import com.lasthopesoftware.threading.ISimpleTask;
import com.lasthopesoftware.threading.ISimpleTask.OnExecuteListener;
import com.lasthopesoftware.threading.SimpleTask;

public class FileSystem extends ItemAsyncBase<IItem<?>> implements IItem<IItem<?>> {
	private TreeSet<IItem<?>> mVisibleViews;
	private Playlists mPlaylistsView;
	private int[] mVisibleViewKeys;
	
	private ArrayList<OnCompleteListener<List<IItem<?>>>> mOnCompleteClientListeners;
	private OnStartListener<List<IItem<?>>> mOnStartListener;
	private static final OnConnectListener<List<IItem<?>>> mOnConnectListener = new OnConnectListener<List<IItem<?>>>() {
		
		@Override
		public List<IItem<?>> onConnect(InputStream is) {
			ArrayList<IItem<?>> returnList = new ArrayList<IItem<?>>();
			for (Item item : FilesystemResponse.GetItems(is))
				returnList.add(item);
			
			return returnList;
		}
	};
	
	private OnErrorListener<List<IItem<?>>> mOnErrorListener;
	
	private static Object syncObject = new Object();
	private static int mInstanceVisibleViewKey;
	private static FileSystem mInstance;
	
	public final static synchronized FileSystem getInstance(final Context context) {
		final int storedSelectedViewKey = LibrarySession.GetLibrary(context).getSelectedView();
		if (storedSelectedViewKey != mInstanceVisibleViewKey)
			mInstance = new FileSystem(storedSelectedViewKey);
		return mInstance;
	}
	
	private FileSystem(int... visibleViewKeys) {
		super();
		
		mVisibleViewKeys = visibleViewKeys;
	}
	
	public String getSubItemUrl() {
		return ConnectionManager.getFormattedUrl("Browse/Children");
	}
	
	public void setVisibleViews(int... visibleViewKeys) {
		if (Arrays.equals(mVisibleViewKeys, visibleViewKeys)) return;
		mVisibleViewKeys = visibleViewKeys;
		synchronized(syncObject) {
			mVisibleViews = null;
		}
	}
	
	public ArrayList<IItem<?>> getVisibleViews() {
		try {
			return getVisibleViewsTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR).get();
		} catch (Exception e) {
			return new ArrayList<IItem<?>>();
		}
	}
	
	public void getVisibleViewsAsync() {
		getVisibleViewsAsync(null);
	}
	
	public void getVisibleViewsAsync(ISimpleTask.OnCompleteListener<String, Void, ArrayList<IItem<?>>> onCompleteListener) {
		SimpleTask<String, Void, ArrayList<IItem<?>>> getViewsTask = getVisibleViewsTask();
		
		if (onCompleteListener != null) getViewsTask.addOnCompleteListener(onCompleteListener);
		
		getViewsTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}
	
	private SimpleTask<String, Void, ArrayList<IItem<?>>> getVisibleViewsTask() {
		return new SimpleTask<String, Void, ArrayList<IItem<?>>>(new OnExecuteListener<String, Void, ArrayList<IItem<?>>>() {
			
			@Override
			public ArrayList<IItem<?>> onExecute(ISimpleTask<String, Void, ArrayList<IItem<?>>> owner, String... params) throws Exception {
				synchronized(syncObject) {
					if (mVisibleViews == null || mVisibleViews.size() == 0) {
						List<IItem<?>> libraries = getSubItems();
						mVisibleViews = new TreeSet<IItem<?>>(new Comparator<IItem<?>>() {
	
							@Override
							public int compare(IItem<?> lhs, IItem<?> rhs) {
								return lhs.getKey() - rhs.getKey();
							}
						});
						
						for (int viewKey : mVisibleViewKeys) {
							for (IItem<?> library : libraries) {
								if (mVisibleViewKeys.length > 0 && viewKey != library.getKey()) continue;
								
								if (library.getValue().equalsIgnoreCase("Playlists")) {
									if (mPlaylistsView == null) mPlaylistsView = new Playlists(Integer.MAX_VALUE);
									mVisibleViews.add(new Playlists(Integer.MAX_VALUE));
									continue;
								}
								
								for (IItem<?> view : library.getSubItems())
									mVisibleViews.add(view);
							}
						}
					}
				}
				
				return new ArrayList<IItem<?>>(mVisibleViews);
			}
		});
	}

	@Override
	public void setOnItemsStartListener(OnStartListener<List<IItem<?>>> listener) {
		mOnStartListener = listener;
	}

	@Override
	public void setOnItemsErrorListener(OnErrorListener<List<IItem<?>>> listener) {
		mOnErrorListener = listener;
	}

	@Override
	protected OnConnectListener<List<IItem<?>>> getOnItemConnectListener() {
		return mOnConnectListener;
	}

	@Override
	protected List<OnCompleteListener<List<IItem<?>>>> getOnItemsCompleteListeners() {
		return mOnCompleteClientListeners;
	}

	@Override
	protected List<OnStartListener<List<IItem<?>>>> getOnItemsStartListeners() {
		LinkedList<OnStartListener<List<IItem<?>>>> listeners = new LinkedList<OnStartListener<List<IItem<?>>>>();
		if (mOnStartListener != null) listeners.add(mOnStartListener);
		return listeners;
	}

	@Override
	protected List<OnErrorListener<List<IItem<?>>>> getOnItemsErrorListeners() {
		LinkedList<OnErrorListener<List<IItem<?>>>> listeners = new LinkedList<OnErrorListener<List<IItem<?>>>>();
		if (mOnErrorListener != null) listeners.add(mOnErrorListener);
		return listeners;
	}

	@Override
	public String[] getSubItemParams() {
		return new String[] { "Browse/Children" };
	}

	@Override
	public int compareTo(IItem<?> another) {
		return 0;
	}

	@Override
	public void addOnItemsCompleteListener(OnCompleteListener<List<IItem<?>>> listener) {
		if (mOnCompleteClientListeners == null) mOnCompleteClientListeners = new ArrayList<OnCompleteListener<List<IItem<?>>>>();
		
		mOnCompleteClientListeners.add(listener);
	}

	@Override
	public void removeOnItemsCompleteListener(OnCompleteListener<List<IItem<?>>> listener) {
		if (mOnCompleteClientListeners != null)
			mOnCompleteClientListeners.remove(listener);
	}
}

