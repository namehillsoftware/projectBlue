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
import com.lasthopesoftware.bluewater.client.connection.SessionConnection;
import com.lasthopesoftware.bluewater.client.library.access.ILibraryProvider;
import com.lasthopesoftware.bluewater.client.library.access.LibraryRepository;
import com.lasthopesoftware.bluewater.client.library.items.IItem;
import com.lasthopesoftware.bluewater.client.library.items.Item;
import com.lasthopesoftware.bluewater.client.library.items.access.ItemProvider;
import com.lasthopesoftware.bluewater.client.library.items.list.menus.changes.handlers.IItemListMenuChangeHandler;
import com.lasthopesoftware.bluewater.client.library.items.stored.StoredItemAccess;
import com.lasthopesoftware.bluewater.client.library.views.handlers.OnGetLibraryViewItemResultsComplete;
import com.lasthopesoftware.bluewater.client.servers.selection.ISelectedLibraryIdentifierProvider;
import com.lasthopesoftware.bluewater.client.servers.selection.SelectedBrowserLibraryIdentifierProvider;
import com.lasthopesoftware.bluewater.shared.promises.resolutions.Dispatch;
import com.vedsoft.futures.callables.VoidFunc;
import com.vedsoft.futures.runnables.OneParameterAction;

import java.util.List;

import static com.vedsoft.futures.callables.VoidFunc.runningCarelessly;

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
			.then(Dispatch.toContext(runningCarelessly(activeLibrary -> {
				final OneParameterAction<List<Item>> onGetVisibleViewsCompleteListener = result -> {
					if (result == null || result.size() == 0) return;

					final int categoryPosition = getArguments().getInt(ARG_CATEGORY_POSITION);
					final IItem category = categoryPosition < result.size() ? result.get(categoryPosition) : result.get(result.size() - 1);

					layout.addView(BuildStandardItemView(activity, container, categoryPosition, category, pbLoading));
				};

				ItemProvider
					.provide(SessionConnection.getSessionConnectionProvider(), activeLibrary.getSelectedView())
					.onComplete(onGetVisibleViewsCompleteListener)
					.onError(new HandleViewIoException<>(activity, new Runnable() {

						@Override
						public void run() {
							ItemProvider
								.provide(SessionConnection.getSessionConnectionProvider(), activeLibrary.getSelectedView())
								.onComplete(onGetVisibleViewsCompleteListener)
								.onError(new HandleViewIoException<>(activity, this))
								.execute();
						}
					}))
					.execute();
	    }), activity));

        return layout;
    }

	private ListView BuildStandardItemView(final Activity activity, final ViewGroup container, final int position, final IItem category, final View loadingView) {
		final ListView listView = new ListView(activity);
    	listView.setVisibility(View.INVISIBLE);

		final ISelectedLibraryIdentifierProvider selectedLibraryIdentifierProvider = new SelectedBrowserLibraryIdentifierProvider(getContext());
		final ILibraryProvider libraryProvider = new LibraryRepository(getContext());

		libraryProvider
			.getLibrary(selectedLibraryIdentifierProvider.getSelectedLibraryId())
			.then(Dispatch.toContext(VoidFunc.runningCarelessly(library -> {
				final OnGetLibraryViewItemResultsComplete onGetLibraryViewItemResultsComplete =
					new OnGetLibraryViewItemResultsComplete(
						activity,
						container,
						listView,
						loadingView,
						position,
						itemListMenuChangeHandler,
						new StoredItemAccess(activity, library),
						library);

				ItemProvider
					.provide(SessionConnection.getSessionConnectionProvider(), category.getKey())
					.onComplete(onGetLibraryViewItemResultsComplete)
					.onError(new HandleViewIoException<>(activity, new Runnable() {

						@Override
						public void run() {
							ItemProvider
								.provide(SessionConnection.getSessionConnectionProvider(), category.getKey())
								.onComplete(onGetLibraryViewItemResultsComplete)
								.onError(new HandleViewIoException<>(activity, this))
								.execute();
						}
					}))
					.execute();
			}), activity));

		return listView;
	}

	public void setOnItemListMenuChangeHandler(IItemListMenuChangeHandler itemListMenuChangeHandler) {
		this.itemListMenuChangeHandler = itemListMenuChangeHandler;
	}
}