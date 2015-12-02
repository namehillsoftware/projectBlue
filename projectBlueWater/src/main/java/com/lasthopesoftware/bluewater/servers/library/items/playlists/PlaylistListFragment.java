package com.lasthopesoftware.bluewater.servers.library.items.playlists;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.servers.connection.HandleViewIoException;
import com.lasthopesoftware.bluewater.servers.connection.SessionConnection;
import com.lasthopesoftware.bluewater.servers.library.items.list.menus.changes.handlers.IItemListMenuChangeHandler;
import com.lasthopesoftware.bluewater.servers.library.items.playlists.access.PlaylistsProvider;
import com.lasthopesoftware.bluewater.servers.library.views.handlers.OnGetLibraryViewIItemResultsComplete;
import com.lasthopesoftware.bluewater.servers.library.views.handlers.OnGetLibraryViewPlaylistResultsComplete;

import java.util.List;

/**
 * Created by david on 11/29/15.
 */
public class PlaylistListFragment extends Fragment {

	private IItemListMenuChangeHandler itemListMenuChangeHandler;

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		final RelativeLayout itemListLayout = (RelativeLayout) inflater.inflate(R.layout.activity_view_items, container, false);

		final ListView playlistView = (ListView) itemListLayout.findViewById(R.id.lvItems);
		final ProgressBar loadingView = (ProgressBar) itemListLayout.findViewById(R.id.pbLoadingItems);

		playlistView.setVisibility(View.INVISIBLE);

		final OnGetLibraryViewIItemResultsComplete<Playlist> onGetLibraryViewPlaylistResultsComplete = new OnGetLibraryViewPlaylistResultsComplete(getActivity(), container, playlistView, loadingView, 0, itemListMenuChangeHandler);

		final PlaylistsProvider playlistsProvider = new PlaylistsProvider(SessionConnection.getSessionConnectionProvider());
		playlistsProvider
				.onComplete(onGetLibraryViewPlaylistResultsComplete)
				.onError(new HandleViewIoException<Void, Void, List<Playlist>>(getActivity(), new Runnable() {

					@Override
					public void run() {
						final PlaylistsProvider playlistsProvider = new PlaylistsProvider(SessionConnection.getSessionConnectionProvider());

						playlistsProvider
								.onComplete(onGetLibraryViewPlaylistResultsComplete)
								.onError(new HandleViewIoException<Void, Void, List<Playlist>>(getActivity(), this))
								.execute();
					}
				}))
				.execute();

		return itemListLayout;
	}

	public void setOnItemListMenuChangeHandler(IItemListMenuChangeHandler itemListMenuChangeHandler) {
		this.itemListMenuChangeHandler = itemListMenuChangeHandler;
	}
}
