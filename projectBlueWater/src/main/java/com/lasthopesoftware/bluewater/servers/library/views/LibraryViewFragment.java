package com.lasthopesoftware.bluewater.servers.library.views;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import com.lasthopesoftware.bluewater.servers.connection.HandleViewIoException;
import com.lasthopesoftware.bluewater.servers.connection.SessionConnection;
import com.lasthopesoftware.bluewater.servers.library.FileSystem;
import com.lasthopesoftware.bluewater.servers.library.items.IItem;
import com.lasthopesoftware.bluewater.servers.library.items.Item;
import com.lasthopesoftware.bluewater.servers.library.items.access.ItemProvider;
import com.lasthopesoftware.bluewater.servers.library.items.list.menus.changes.handlers.IItemListMenuChangeHandler;
import com.lasthopesoftware.bluewater.servers.library.items.playlists.Playlist;
import com.lasthopesoftware.bluewater.servers.library.items.playlists.Playlists;
import com.lasthopesoftware.bluewater.servers.library.items.playlists.access.PlaylistsProvider;
import com.lasthopesoftware.bluewater.servers.library.repository.Library;
import com.lasthopesoftware.bluewater.servers.library.repository.LibrarySession;
import com.lasthopesoftware.bluewater.servers.library.views.handlers.OnGetLibraryViewIItemResultsComplete;
import com.lasthopesoftware.bluewater.servers.library.views.handlers.OnGetLibraryViewItemResultsComplete;
import com.lasthopesoftware.bluewater.servers.library.views.handlers.OnGetLibraryViewPlaylistResultsComplete;
import com.lasthopesoftware.threading.ISimpleTask;

import java.util.ArrayList;

public class LibraryViewFragment extends Fragment {

    private static final String ARG_CATEGORY_POSITION = "category_position";

	private IItemListMenuChangeHandler itemListMenuChangeHandler;

	public static LibraryViewFragment getPreparedFragment(final int libraryViewId) {
        final LibraryViewFragment returnFragment = new LibraryViewFragment();
        final Bundle args = new Bundle();
        args.putInt(LibraryViewFragment.ARG_CATEGORY_POSITION, libraryViewId);
        returnFragment.setArguments(args);
        return returnFragment;
    }

    @Override
    @SuppressWarnings("unchecked")
    public View onCreateView(LayoutInflater inflater, final ViewGroup container, Bundle savedInstanceState) {
    	final Activity activity = getActivity();

    	final RelativeLayout layout = new RelativeLayout(activity);
    	layout.setLayoutParams(new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

    	final ProgressBar pbLoading = new ProgressBar(activity, null, android.R.attr.progressBarStyleLarge);
    	final RelativeLayout.LayoutParams pbParams = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
    	pbParams.addRule(RelativeLayout.CENTER_IN_PARENT);
    	pbLoading.setLayoutParams(pbParams);
    	layout.addView(pbLoading);

    	LibrarySession.GetActiveLibrary(activity, new ISimpleTask.OnCompleteListener<Integer, Void, Library>() {
		    @Override
		    public void onComplete(ISimpleTask<Integer, Void, Library> owner, Library library) {
			    final ISimpleTask.OnCompleteListener<String, Void, ArrayList<IItem>> onGetVisibleViewsCompleteListener = new ISimpleTask.OnCompleteListener<String, Void, ArrayList<IItem>>() {
				
				    @Override
				    public void onComplete(ISimpleTask<String, Void, ArrayList<IItem>> owner, ArrayList<IItem> result) {
					    if (result == null || result.size() == 0) return;
					
					    final int categoryPosition = getArguments().getInt(ARG_CATEGORY_POSITION);
					    final IItem category = categoryPosition < result.size() ? result.get(categoryPosition) : result.get(result.size() - 1);
					
					    if (category instanceof Playlists)
						    layout.addView(BuildPlaylistView(activity, container, categoryPosition, pbLoading));
					    else if (category instanceof Item)
						    layout.addView(BuildStandardItemView(activity, container, categoryPosition, (Item) category, pbLoading));
				    }
			    };
			
			    final HandleViewIoException handleViewIoException = new HandleViewIoException(activity, new Runnable() {
				
				    @Override
				    public void run() {
					    final Runnable _this = this;
					
					    LibrarySession.GetActiveLibrary(activity, new ISimpleTask.OnCompleteListener<Integer, Void, Library>() {
						    @Override
						    public void onComplete(ISimpleTask<Integer, Void, Library> owner, Library library) {
							    final FileSystem fileSystem = new FileSystem(SessionConnection.getSessionConnectionProvider(), library);
							    fileSystem.getVisibleViewsAsync(onGetVisibleViewsCompleteListener, new HandleViewIoException(activity, _this));
						    }
					    });
				    }
			    });
			
			    final FileSystem fileSystem = new FileSystem(SessionConnection.getSessionConnectionProvider(), library);
			    fileSystem.getVisibleViewsAsync(onGetVisibleViewsCompleteListener, handleViewIoException);
		    }
	    });

        return layout;
    }

	@SuppressWarnings("unchecked")
	private ListView BuildPlaylistView(final Activity activity, final ViewGroup container, final int position, final View loadingView) {

		final ListView listView = new ListView(activity);
		listView.setVisibility(View.INVISIBLE);

		final OnGetLibraryViewIItemResultsComplete<Playlist> onGetLibraryViewPlaylistResultsComplete = new OnGetLibraryViewPlaylistResultsComplete(activity, container, listView, loadingView, position, itemListMenuChangeHandler);

		final PlaylistsProvider playlistsProvider = new PlaylistsProvider(SessionConnection.getSessionConnectionProvider());
		playlistsProvider
			.onComplete(onGetLibraryViewPlaylistResultsComplete)
			.onError(new HandleViewIoException(activity, new Runnable() {

				@Override
				public void run() {
					final PlaylistsProvider playlistsProvider = new PlaylistsProvider(SessionConnection.getSessionConnectionProvider());

					playlistsProvider
							.onComplete(onGetLibraryViewPlaylistResultsComplete)
							.onError(new HandleViewIoException(activity, this))
							.execute();
				}
			}))
			.execute();

		return listView;
    }

	@SuppressWarnings("unchecked")
	private ListView BuildStandardItemView(final Activity activity, final ViewGroup container, final int position, final Item category, final View loadingView) {
		final ListView listView = new ListView(activity);
    	listView.setVisibility(View.INVISIBLE);

		final OnGetLibraryViewItemResultsComplete onGetLibraryViewItemResultsComplete = new OnGetLibraryViewItemResultsComplete(activity, container, listView, loadingView, position, itemListMenuChangeHandler);

		ItemProvider
				.provide(SessionConnection.getSessionConnectionProvider(), category.getKey())
				.onComplete(onGetLibraryViewItemResultsComplete)
				.onError(new HandleViewIoException(activity, new Runnable() {

					@Override
					public void run() {
							ItemProvider
								.provide(SessionConnection.getSessionConnectionProvider(), category.getKey())
								.onComplete(onGetLibraryViewItemResultsComplete)
								.onError(new HandleViewIoException(activity, this))
									.execute();
					}
				}))
				.execute();

		return listView;
	}

	public void setOnItemListMenuChangeHandler(IItemListMenuChangeHandler itemListMenuChangeHandler) {
		this.itemListMenuChangeHandler = itemListMenuChangeHandler;
	}

}