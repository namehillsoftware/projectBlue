package com.lasthopesoftware.bluewater.servers.library;

import android.content.Context;
import android.os.AsyncTask;

import com.lasthopesoftware.bluewater.disk.sqlite.access.LibrarySession;
import com.lasthopesoftware.bluewater.disk.sqlite.objects.Library;
import com.lasthopesoftware.bluewater.servers.connection.ConnectionProvider;
import com.lasthopesoftware.bluewater.servers.library.access.LibraryViewsProvider;
import com.lasthopesoftware.bluewater.servers.library.items.IItem;
import com.lasthopesoftware.bluewater.servers.library.items.Item;
import com.lasthopesoftware.bluewater.servers.library.items.access.ItemProvider;
import com.lasthopesoftware.bluewater.servers.library.items.playlists.Playlists;
import com.lasthopesoftware.bluewater.servers.library.items.playlists.access.PlaylistsProvider;
import com.lasthopesoftware.bluewater.shared.AbstractIntKeyStringValue;
import com.lasthopesoftware.threading.ISimpleTask;
import com.lasthopesoftware.threading.ISimpleTask.OnExecuteListener;
import com.lasthopesoftware.threading.SimpleTask;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;

public class FileSystem extends AbstractIntKeyStringValue implements IItem {
	private TreeSet<IItem> mVisibleViews;
	private int[] mVisibleViewKeys;

    public FileSystem(Library library) {
        this(library.getSelectedView());
    }

	private FileSystem(int... visibleViewKeys) {
		super();
		
		mVisibleViewKeys = visibleViewKeys;
	}
	
	public String getSubItemUrl() {
		return ConnectionProvider.getFormattedUrl("Browse/Children");
	}
	
	public void setVisibleViews(int... visibleViewKeys) {
		if (Arrays.equals(mVisibleViewKeys, visibleViewKeys)) return;
		mVisibleViewKeys = visibleViewKeys;
		mVisibleViews = null;
	}

	public void getVisibleViewsAsync() {
		getVisibleViewsAsync(null);
	}
	
	public void getVisibleViewsAsync(ISimpleTask.OnCompleteListener<String, Void, ArrayList<IItem>> onCompleteListener) {
		getVisibleViewsAsync(onCompleteListener, null);
	}
	
	public void getVisibleViewsAsync(ISimpleTask.OnCompleteListener<String, Void, ArrayList<IItem>> onCompleteListener,
									 ISimpleTask.OnErrorListener<String, Void, ArrayList<IItem>> onErrorListener) {
        getVisibleViewsTask(onCompleteListener, onErrorListener).execute(AsyncTask.THREAD_POOL_EXECUTOR);
	}
	
	private SimpleTask<String, Void, ArrayList<IItem>> getVisibleViewsTask(ISimpleTask.OnCompleteListener<String, Void, ArrayList<IItem>> onCompleteListener, final ISimpleTask.OnErrorListener<String, Void, ArrayList<IItem>> onErrorListener) {
        final FileSystem fileSystem = this;
		final SimpleTask<String, Void, ArrayList<IItem>> getViewsTask = new SimpleTask<String, Void, ArrayList<IItem>>(new OnExecuteListener<String, Void, ArrayList<IItem>>() {
			
			@Override
			public ArrayList<IItem> onExecute(final ISimpleTask<String, Void, ArrayList<IItem>> thisTask, String... params) throws Exception {

				if (mVisibleViews == null || mVisibleViews.size() == 0) {
					mVisibleViews = new TreeSet<IItem>(new Comparator<IItem>() {

						@Override
						public int compare(IItem lhs, IItem rhs) {
							return lhs.getKey() - rhs.getKey();
						}
					});

                    final LibraryViewsProvider libraryViewsProvider = new LibraryViewsProvider();
					final List<Item> libraries = libraryViewsProvider.get();

                    if (libraryViewsProvider.getException() != null)
                        throw libraryViewsProvider.getException();

					for (int viewKey : mVisibleViewKeys) {
						for (Item library : libraries) {
							if (mVisibleViewKeys.length > 0 && viewKey != library.getKey()) continue;
							
							if (library.getValue().equalsIgnoreCase("Playlists")) {
								mVisibleViews.add(new Playlists(Integer.MAX_VALUE, (new PlaylistsProvider()).get()));
								continue;
							}
							
							final List<Item> views = ItemProvider.provide(library.getKey()).get();
							for (Item view : views)
								mVisibleViews.add(view);
						}
					}
				}
				
				return new ArrayList<IItem>(mVisibleViews);
			}
		});

        if (onCompleteListener != null) getViewsTask.addOnCompleteListener(onCompleteListener);
        if (onErrorListener != null) getViewsTask.addOnErrorListener(onErrorListener);

        return getViewsTask;
	}

	@Override
	public String[] getSubItemParams() {
		return new String[] { "Browse/Children" };
	}

	@Override
	public int compareTo(IItem another) {
		return 0;
	}
	
	public interface OnGetFileSystemCompleteListener {
		void onGetFileSystemComplete(FileSystem fileSystem);
	}
	
	public static class Instance {

		private static int mInstanceVisibleViewKey = -1;
		private static FileSystem mInstance = null;
		
		public final static void get(final Context context, final OnGetFileSystemCompleteListener onGetFileSystemCompleteListener) {
			if (onGetFileSystemCompleteListener == null)
				throw new IllegalArgumentException("onGetFileSystemCompleteListener cannot be null.");
			
			LibrarySession.GetLibrary(context, new ISimpleTask.OnCompleteListener<Integer, Void, Library>() {
				
				@Override
				public void onComplete(ISimpleTask<Integer, Void, Library> owner, Library result) {
					onGetFileSystemCompleteListener.onGetFileSystemComplete(get(result));
				}
			});
		}
		
		public final static synchronized FileSystem get(final Library library) {
			final int storedSelectedViewKey = library.getSelectedView();
			if (mInstance == null || storedSelectedViewKey != mInstanceVisibleViewKey) {
				mInstance = new FileSystem(storedSelectedViewKey);
				mInstanceVisibleViewKey = storedSelectedViewKey;
			}
			
			return mInstance;
		}
	}
}

