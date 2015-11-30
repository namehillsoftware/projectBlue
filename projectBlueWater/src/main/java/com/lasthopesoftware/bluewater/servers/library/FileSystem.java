package com.lasthopesoftware.bluewater.servers.library;

import android.os.AsyncTask;
import android.util.SparseArray;

import com.lasthopesoftware.bluewater.servers.connection.ConnectionProvider;
import com.lasthopesoftware.bluewater.servers.library.access.LibraryViewsProvider;
import com.lasthopesoftware.bluewater.servers.library.items.IItem;
import com.lasthopesoftware.bluewater.servers.library.items.Item;
import com.lasthopesoftware.bluewater.servers.library.items.access.ItemProvider;
import com.lasthopesoftware.bluewater.servers.library.repository.Library;
import com.lasthopesoftware.bluewater.shared.AbstractIntKeyStringValue;
import com.lasthopesoftware.threading.ISimpleTask;
import com.lasthopesoftware.threading.ISimpleTask.OnExecuteListener;
import com.lasthopesoftware.threading.SimpleTask;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class FileSystem extends AbstractIntKeyStringValue implements IItem {

	private static class ViewsHolder {
		public final Set<Integer> visibleViewKeys;
		public final TreeSet<IItem> visibleViews;
		public ViewsHolder(Set<Integer> visibleViewKeys, TreeSet<IItem> visibleViews) {
			this.visibleViewKeys = visibleViewKeys;
			this.visibleViews = visibleViews;
		}

	}

	private static final SparseArray<ViewsHolder> viewsCache = new SparseArray<>();

	private final Set<Integer> visibleViewKeys;
	private final ConnectionProvider connectionProvider;
	private final Library library;

    public FileSystem(ConnectionProvider connectionProvider, Library library) {
        super();

	    this.connectionProvider = connectionProvider;
	    this.library = library;
	    this.visibleViewKeys = new HashSet<>(1);
		this.visibleViewKeys.add(library.getSelectedView());
    }
	
	public void getVisibleViewsAsync(ISimpleTask.OnCompleteListener<String, Void, ArrayList<IItem>> onCompleteListener) {
		getVisibleViewsAsync(onCompleteListener, null);
	}
	
	public void getVisibleViewsAsync(ISimpleTask.OnCompleteListener<String, Void, ArrayList<IItem>> onCompleteListener,
									 ISimpleTask.OnErrorListener<String, Void, ArrayList<IItem>> onErrorListener) {
        getVisibleViewsTask(onCompleteListener, onErrorListener).execute(AsyncTask.THREAD_POOL_EXECUTOR);
	}
	
	private SimpleTask<String, Void, ArrayList<IItem>> getVisibleViewsTask(ISimpleTask.OnCompleteListener<String, Void, ArrayList<IItem>> onCompleteListener, final ISimpleTask.OnErrorListener<String, Void, ArrayList<IItem>> onErrorListener) {
		final SimpleTask<String, Void, ArrayList<IItem>> getViewsTask = new SimpleTask<>(new OnExecuteListener<String, Void, ArrayList<IItem>>() {
			
			@Override
			public ArrayList<IItem> onExecute(final ISimpleTask<String, Void, ArrayList<IItem>> thisTask, String... params) throws Exception {

				ViewsHolder viewsHolder = viewsCache.get(library.getId());

				if (viewsHolder != null && viewsHolder.visibleViewKeys.size() == visibleViewKeys.size() && viewsHolder.visibleViewKeys.containsAll(visibleViewKeys)) {
					final TreeSet<IItem> visibleViews = viewsHolder.visibleViews;
					if (visibleViews != null && visibleViews.size() != 0)
						return new ArrayList<>(visibleViews);
				}

				final TreeSet<IItem> visibleViews = new TreeSet<>(new Comparator<IItem>() {

					@Override
					public int compare(IItem lhs, IItem rhs) {
						return lhs.getKey() - rhs.getKey();
					}
				});

				final LibraryViewsProvider libraryViewsProvider = new LibraryViewsProvider(connectionProvider);
				final List<Item> libraryViews = libraryViewsProvider.get();

				if (libraryViewsProvider.getException() != null)
				    throw libraryViewsProvider.getException();

				for (int viewKey : visibleViewKeys) {
					for (Item libraryView : libraryViews) {
						if (visibleViewKeys.size() > 0 && viewKey != libraryView.getKey()) continue;
						if (libraryView.getValue().equalsIgnoreCase("Playlists")) continue;

						final List<Item> views = ItemProvider.provide(connectionProvider, libraryView.getKey()).get();
						for (Item view : views)
							visibleViews.add(view);
					}
				}

				viewsCache.put(library.getId(), new ViewsHolder(visibleViewKeys, visibleViews));
				
				return new ArrayList<>(visibleViews);
			}
		});

        if (onCompleteListener != null) getViewsTask.addOnCompleteListener(onCompleteListener);
        if (onErrorListener != null) getViewsTask.addOnErrorListener(onErrorListener);

        return getViewsTask;
	}

	@Override
	public int compareTo(IItem another) {
		return 0;
	}
}

