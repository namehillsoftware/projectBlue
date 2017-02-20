package com.lasthopesoftware.bluewater.client.library.items.playlists;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.client.connection.HandleViewIoException;
import com.lasthopesoftware.bluewater.client.connection.SessionConnection;
import com.lasthopesoftware.bluewater.client.library.access.ILibraryProvider;
import com.lasthopesoftware.bluewater.client.library.access.LibraryRepository;
import com.lasthopesoftware.bluewater.client.library.items.list.menus.changes.handlers.IItemListMenuChangeHandler;
import com.lasthopesoftware.bluewater.client.library.items.playlists.access.PlaylistsProvider;
import com.lasthopesoftware.bluewater.client.library.items.stored.StoredItemAccess;
import com.lasthopesoftware.bluewater.client.library.views.handlers.OnGetLibraryViewIItemResultsComplete;
import com.lasthopesoftware.bluewater.client.library.views.handlers.OnGetLibraryViewPlaylistResultsComplete;
import com.lasthopesoftware.bluewater.client.servers.selection.ISelectedLibraryIdentifierProvider;
import com.lasthopesoftware.bluewater.client.servers.selection.SelectedBrowserLibraryIdentifierProvider;
import com.lasthopesoftware.bluewater.shared.promises.resolutions.Dispatch;
import com.vedsoft.futures.callables.VoidFunc;

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

		final ISelectedLibraryIdentifierProvider selectedLibraryIdentifierProvider = new SelectedBrowserLibraryIdentifierProvider(getContext());
		final ILibraryProvider libraryProvider = new LibraryRepository(getContext());

		libraryProvider
			.getLibrary(selectedLibraryIdentifierProvider.getSelectedLibraryId())
			.then(Dispatch.toContext(VoidFunc.runningCarelessly(library -> {
				final OnGetLibraryViewIItemResultsComplete<Playlist> onGetLibraryViewPlaylistResultsComplete =
					new OnGetLibraryViewPlaylistResultsComplete(
						getActivity(),
						container,
						playlistView,
						loadingView,
						0,
						itemListMenuChangeHandler,
						new StoredItemAccess(getContext(), library),
						library);

				final PlaylistsProvider playlistsProvider = new PlaylistsProvider(SessionConnection.getSessionConnectionProvider());
				playlistsProvider
					.onComplete(onGetLibraryViewPlaylistResultsComplete)
					.onError(new HandleViewIoException<>(getActivity(), new Runnable() {

						@Override
						public void run() {
							final PlaylistsProvider playlistsProvider = new PlaylistsProvider(SessionConnection.getSessionConnectionProvider());

							playlistsProvider
								.onComplete(onGetLibraryViewPlaylistResultsComplete)
								.onError(new HandleViewIoException<>(getActivity(), this))
								.execute();
						}
					}))
					.execute();
			}), getContext()));

		return itemListLayout;
	}

	public void setOnItemListMenuChangeHandler(IItemListMenuChangeHandler itemListMenuChangeHandler) {
		this.itemListMenuChangeHandler = itemListMenuChangeHandler;
	}
}
