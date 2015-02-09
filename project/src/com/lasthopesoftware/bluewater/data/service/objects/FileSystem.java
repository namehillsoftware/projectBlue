package com.lasthopesoftware.bluewater.data.service.objects;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;

import android.content.Context;
import android.os.AsyncTask;

import com.lasthopesoftware.bluewater.data.service.access.FilesystemResponse;
import com.lasthopesoftware.bluewater.data.service.access.IDataTask.OnCompleteListener;
import com.lasthopesoftware.bluewater.data.service.access.IDataTask.OnConnectListener;
import com.lasthopesoftware.bluewater.data.service.access.IDataTask.OnErrorListener;
import com.lasthopesoftware.bluewater.data.service.access.IDataTask.OnStartListener;
import com.lasthopesoftware.bluewater.data.service.access.connection.ConnectionManager;
import com.lasthopesoftware.bluewater.data.sqlite.access.LibrarySession;
import com.lasthopesoftware.bluewater.data.sqlite.objects.Library;
import com.lasthopesoftware.bluewater.servers.library.items.ItemProvider;
import com.lasthopesoftware.bluewater.servers.library.items.playlists.Playlists;
import com.lasthopesoftware.bluewater.servers.library.items.playlists.PlaylistsProvider;
import com.lasthopesoftware.threading.ISimpleTask;
import com.lasthopesoftware.threading.ISimpleTask.OnExecuteListener;
import com.lasthopesoftware.threading.SimpleTask;

public class FileSystem extends ItemAsyncBase implements IItem {
	private TreeSet<IItem> mVisibleViews;
//	private Playlists mPlaylistsView;
	private int[] mVisibleViewKeys;
	
	private ArrayList<OnCompleteListener<List<IItem>>> mOnCompleteClientListeners;
	private OnStartListener<List<IItem>> mOnStartListener;
	private static final OnConnectListener<List<IItem>> mOnGetSubItemsConnectListener = new OnConnectListener<List<IItem>>() {
		
		@Override
		public List<IItem> onConnect(InputStream is) {
			return new ArrayList<IItem>(FilesystemResponse.GetItems(is));
		}
	};
	
	private OnErrorListener<List<IItem>> mOnErrorListener;
	
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
		mVisibleViews = null;
	}
	
	public ArrayList<IItem> getVisibleViews() {
		try {
			return getVisibleViewsTask().execute(AsyncTask.THREAD_POOL_EXECUTOR).get();
		} catch (Exception e) {
			return new ArrayList<IItem>();
		}
	}
	
	public void getVisibleViewsAsync() {
		getVisibleViewsAsync(null);
	}
	
	public void getVisibleViewsAsync(ISimpleTask.OnCompleteListener<String, Void, ArrayList<IItem>> onCompleteListener) {
		getVisibleViewsAsync(onCompleteListener, null);
	}
	
	public void getVisibleViewsAsync(ISimpleTask.OnCompleteListener<String, Void, ArrayList<IItem>> onCompleteListener,
									 ISimpleTask.OnErrorListener<String, Void, ArrayList<IItem>> onErrorListener) {
		final SimpleTask<String, Void, ArrayList<IItem>> getViewsTask = getVisibleViewsTask();
		
		if (onCompleteListener != null) getViewsTask.addOnCompleteListener(onCompleteListener);
		if (onErrorListener != null) getViewsTask.addOnErrorListener(onErrorListener);
		
		getViewsTask.execute(AsyncTask.THREAD_POOL_EXECUTOR);
	}
	
	private SimpleTask<String, Void, ArrayList<IItem>> getVisibleViewsTask() {
		return new SimpleTask<String, Void, ArrayList<IItem>>(new OnExecuteListener<String, Void, ArrayList<IItem>>() {
			
			@Override
			public ArrayList<IItem> onExecute(ISimpleTask<String, Void, ArrayList<IItem>> owner, String... params) throws Exception {

				if (mVisibleViews == null || mVisibleViews.size() == 0) {
					mVisibleViews = new TreeSet<IItem>(new Comparator<IItem>() {

						@Override
						public int compare(IItem lhs, IItem rhs) {
							return lhs.getKey() - rhs.getKey();
						}
					});
					
					final List<Item> libraries = (new ItemProvider(getSubItemParams())).get(); 			
					for (int viewKey : mVisibleViewKeys) {
						for (Item library : libraries) {
							if (mVisibleViewKeys.length > 0 && viewKey != library.getKey()) continue;
							
							if (library.getValue().equalsIgnoreCase("Playlists")) {
//								if (mPlaylistsView == null) mPlaylistsView = new Playlists(Integer.MAX_VALUE);
								
								mVisibleViews.add(new Playlists(Integer.MAX_VALUE, (new PlaylistsProvider("Playlists/List")).get()));
								continue;
							}
							
							final List<Item> views = (new ItemProvider(library.getSubItemParams())).get();
							for (Item view : views)
								mVisibleViews.add(view);
						}
					}
				}
				
				return new ArrayList<IItem>(mVisibleViews);
			}
		});
	}
//
//	@Override
//	public void setOnItemsStartListener(OnStartListener<List<IItem>> listener) {
//		mOnStartListener = listener;
//	}
//
//	@Override
//	public void setOnItemsErrorListener(OnErrorListener<List<IItem>> listener) {
//		mOnErrorListener = listener;
//	}
//
//	@Override
//	protected OnConnectListener<List<IItem>> getOnItemConnectListener() {
//		return mOnGetSubItemsConnectListener;
//	}
//
//	@Override
//	protected List<OnCompleteListener<List<IItem>>> getOnItemsCompleteListeners() {
//		return mOnCompleteClientListeners;
//	}
//
//	@Override
//	protected List<OnStartListener<List<IItem>>> getOnItemsStartListeners() {
//		LinkedList<OnStartListener<List<IItem>>> listeners = new LinkedList<OnStartListener<List<IItem>>>();
//		if (mOnStartListener != null) listeners.add(mOnStartListener);
//		return listeners;
//	}
//
//	@Override
//	protected List<OnErrorListener<List<IItem>>> getOnItemsErrorListeners() {
//		LinkedList<OnErrorListener<List<IItem>>> listeners = new LinkedList<OnErrorListener<List<IItem>>>();
//		if (mOnErrorListener != null) listeners.add(mOnErrorListener);
//		return listeners;
//	}

	@Override
	public String[] getSubItemParams() {
		return new String[] { "Browse/Children" };
	}

	@Override
	public int compareTo(IItem another) {
		return 0;
	}

//	@Override
//	public void addOnItemsCompleteListener(OnCompleteListener<List<IItem>> listener) {
//		if (mOnCompleteClientListeners == null) mOnCompleteClientListeners = new ArrayList<OnCompleteListener<List<IItem>>>();
//		
//		mOnCompleteClientListeners.add(listener);
//	}
//
//	@Override
//	public void removeOnItemsCompleteListener(OnCompleteListener<List<IItem>> listener) {
//		if (mOnCompleteClientListeners != null)
//			mOnCompleteClientListeners.remove(listener);
//	}
	
	public interface OnGetFileSystemCompleteListener {
		void onGetFileSystemComplete(FileSystem fileSystem);
	}
	
	public static class Instance implements ISimpleTask.OnCompleteListener<Integer, Void, Library> {

		private static int mInstanceVisibleViewKey = -1;
		private static FileSystem mInstance = null;
		
		private final static Object syncObject = new Object();
		
		private final OnGetFileSystemCompleteListener mOnGetFileSystemCompleteListener;
				
		public final static void get(final Context context, final OnGetFileSystemCompleteListener onGetFileSystemCompleteListener) {
			LibrarySession.GetLibrary(context, new Instance(onGetFileSystemCompleteListener));
		}
		
		private Instance(OnGetFileSystemCompleteListener onGetFileSystemCompleteListener) {
			mOnGetFileSystemCompleteListener = onGetFileSystemCompleteListener;
		}
		
		@Override
		public void onComplete(ISimpleTask<Integer, Void, Library> owner, Library result) {
			synchronized(syncObject) {
				final int storedSelectedViewKey = result.getSelectedView();
				if (mInstance == null || storedSelectedViewKey != mInstanceVisibleViewKey) {
					mInstance = new FileSystem(storedSelectedViewKey);
					mInstanceVisibleViewKey = storedSelectedViewKey;
				}
				
				if (mOnGetFileSystemCompleteListener != null)
					mOnGetFileSystemCompleteListener.onGetFileSystemComplete(mInstance);
			}
			
//			if (mOnGetFileSystemCompleteListener != null)
//				mOnGetFileSystemCompleteListener.onGetFileSystemComplete(new FileSystem(result.getSelectedView()));
		}
		
	}
}

