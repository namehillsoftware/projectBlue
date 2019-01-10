package com.lasthopesoftware.bluewater.client.library.items.list;

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
import com.lasthopesoftware.bluewater.client.connection.HandleViewIoException;
import com.lasthopesoftware.bluewater.client.connection.session.SessionConnection;
import com.lasthopesoftware.bluewater.client.library.access.ILibraryProvider;
import com.lasthopesoftware.bluewater.client.library.access.LibraryRepository;
import com.lasthopesoftware.bluewater.client.library.items.IItem;
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

public class ItemListFragment extends Fragment {

	private static final String ARG_CATEGORY_POSITION = "category_position";

	private IItemListMenuChangeHandler itemListMenuChangeHandler;

	public static ItemListFragment getPreparedFragment(final int libraryViewId) {
		final ItemListFragment returnFragment = new ItemListFragment();
		final Bundle args = new Bundle();
		args.putInt(ItemListFragment.ARG_CATEGORY_POSITION, libraryViewId);
		returnFragment.setArguments(args);
		return returnFragment;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, final ViewGroup container, Bundle savedInstanceState) {
		final Activity activity = getActivity();

		final RelativeLayout layout = new RelativeLayout(activity);
		layout.setLayoutParams(new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

		final ProgressBar pbLoading = new ProgressBar(activity, null, android.R.attr.progressBarStyleLarge);
		final RelativeLayout.LayoutParams pbParams = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		pbParams.addRule(RelativeLayout.CENTER_IN_PARENT);
		pbLoading.setLayoutParams(pbParams);
		layout.addView(pbLoading);

		final ILibraryProvider libraryProvider = new LibraryRepository(activity);
		final ISelectedLibraryIdentifierProvider selectedLibraryIdentifierProvider = new SelectedBrowserLibraryIdentifierProvider(activity);

		libraryProvider
			.getLibrary(selectedLibraryIdentifierProvider.getSelectedLibraryId())
			.then(new VoidResponse<>(activeLibrary -> {
				final PromisedResponse<List<Item>, Void> onGetVisibleViewsCompleteListener = LoopedInPromise.response(result -> {
					if (result == null || result.size() == 0) return null;

					final int categoryPosition = getArguments().getInt(ARG_CATEGORY_POSITION);
					final IItem category = categoryPosition < result.size() ? result.get(categoryPosition) : result.get(result.size() - 1);

					layout.addView(BuildStandardItemView(activity, category, pbLoading));

					return null;
				}, activity);

				final Runnable fillItemsRunnable = new Runnable() {

					@Override
					public void run() {
						SessionConnection.getInstance(activity).promiseSessionConnection()
							.eventually(c -> ItemProvider.provide(c, activeLibrary.getSelectedView()))
							.eventually(onGetVisibleViewsCompleteListener)
							.excuse(new HandleViewIoException(activity, this))
							.excuse(forward())
							.eventually(LoopedInPromise.response(new UnexpectedExceptionToasterResponse(activity), activity));
					}
				};

				fillItemsRunnable.run();
			}));

		return layout;
	}

	private ListView BuildStandardItemView(final Activity activity, final IItem category, final View loadingView) {
		final ListView listView = new ListView(activity);
		listView.setVisibility(View.INVISIBLE);

		final ISelectedLibraryIdentifierProvider selectedLibraryIdentifierProvider = new SelectedBrowserLibraryIdentifierProvider(getContext());
		final ILibraryProvider libraryProvider = new LibraryRepository(getContext());

		libraryProvider
			.getLibrary(selectedLibraryIdentifierProvider.getSelectedLibraryId())
			.then(new VoidResponse<>(library -> {
				PromisedResponse<List<Item>, Void> onGetLibraryViewItemResultsComplete = LoopedInPromise.response(new OnGetLibraryViewItemResultsComplete(
					activity,
					listView,
					loadingView,
					itemListMenuChangeHandler,
					FileListParameters.getInstance(),
					new StoredItemAccess(activity, library),
					library), activity);

				final Runnable fillItemsRunnable = new Runnable() {

					@Override
					public void run() {
						SessionConnection.getInstance(activity).promiseSessionConnection()
							.eventually(c -> ItemProvider.provide(c, category.getKey()))
							.eventually(onGetLibraryViewItemResultsComplete)
							.excuse(new HandleViewIoException(activity, this))
							.excuse(forward())
							.eventually(LoopedInPromise.response(new UnexpectedExceptionToasterResponse(activity), activity));
					}
				};

				fillItemsRunnable.run();
			}));

		return listView;
	}

	public void setOnItemListMenuChangeHandler(IItemListMenuChangeHandler itemListMenuChangeHandler) {
		this.itemListMenuChangeHandler = itemListMenuChangeHandler;
	}
}