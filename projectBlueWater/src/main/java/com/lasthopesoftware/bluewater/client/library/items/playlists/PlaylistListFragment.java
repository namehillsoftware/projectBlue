package com.lasthopesoftware.bluewater.client.library.items.playlists;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.ProgressBar;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.client.connection.HandleViewIoException;
import com.lasthopesoftware.bluewater.client.connection.session.SessionConnection;
import com.lasthopesoftware.bluewater.client.library.access.ILibraryProvider;
import com.lasthopesoftware.bluewater.client.library.access.LibraryRepository;
import com.lasthopesoftware.bluewater.client.library.items.Item;
import com.lasthopesoftware.bluewater.client.library.items.access.ItemProvider;
import com.lasthopesoftware.bluewater.client.library.items.list.menus.changes.handlers.IItemListMenuChangeHandler;
import com.lasthopesoftware.bluewater.client.library.items.media.files.access.parameters.FileListParameters;
import com.lasthopesoftware.bluewater.client.library.views.handlers.OnGetLibraryViewItemResultsComplete;
import com.lasthopesoftware.bluewater.client.servers.selection.ISelectedLibraryIdentifierProvider;
import com.lasthopesoftware.bluewater.client.servers.selection.SelectedBrowserLibraryIdentifierProvider;
import com.lasthopesoftware.bluewater.client.stored.library.items.StoredItemAccess;
import com.lasthopesoftware.bluewater.shared.exceptions.UnexpectedExceptionToasterResponse;
import com.lasthopesoftware.bluewater.shared.promises.extensions.LoopedInPromise;
import com.namehillsoftware.handoff.promises.response.PromisedResponse;
import com.namehillsoftware.handoff.promises.response.VoidResponse;

import java.util.List;

import static com.lasthopesoftware.bluewater.shared.promises.ForwardedResponse.forward;

public class PlaylistListFragment extends Fragment {

	private IItemListMenuChangeHandler itemListMenuChangeHandler;

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.activity_view_items, container, false);
	}

	@Override
	public void onStart() {
		super.onStart();

		final FragmentActivity activity = getActivity();

		if (activity == null) return;

		final ListView playlistView = activity.findViewById(R.id.lvItems);
		final ProgressBar loadingView = activity.findViewById(R.id.pbLoadingItems);

		playlistView.setVisibility(View.INVISIBLE);

		final ISelectedLibraryIdentifierProvider selectedLibraryIdentifierProvider = new SelectedBrowserLibraryIdentifierProvider(getContext());
		final ILibraryProvider libraryProvider = new LibraryRepository(getContext());

		libraryProvider
			.getLibrary(selectedLibraryIdentifierProvider.getSelectedLibraryId())
			.then(new VoidResponse<>(library -> {
				final PromisedResponse<List<Item>, Void> listResolvedPromise =
					LoopedInPromise.response(
						new OnGetLibraryViewItemResultsComplete(
							activity,
							playlistView,
							loadingView,
							itemListMenuChangeHandler,
							FileListParameters.getInstance(),
							new StoredItemAccess(activity, library),
							library), activity);

				final Runnable playlistFillAction = new Runnable() {
					@Override
					public void run() {
						SessionConnection.getInstance(activity).promiseSessionConnection()
							.eventually(c -> ItemProvider.provide(c, library.getSelectedView()))
							.eventually(listResolvedPromise)
							.excuse(new HandleViewIoException(activity, this))
							.excuse(forward())
							.eventually(LoopedInPromise.response(new UnexpectedExceptionToasterResponse(activity), activity));
					}
				};

				playlistFillAction.run();
			}));
	}

	public void setOnItemListMenuChangeHandler(IItemListMenuChangeHandler itemListMenuChangeHandler) {
		this.itemListMenuChangeHandler = itemListMenuChangeHandler;
	}
}
