package com.lasthopesoftware.bluewater.servers.library.items.list;

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
import com.lasthopesoftware.bluewater.servers.library.items.IItem;
import com.lasthopesoftware.bluewater.servers.library.items.Item;
import com.lasthopesoftware.bluewater.servers.library.items.access.ItemProvider;
import com.lasthopesoftware.bluewater.servers.library.items.list.menus.changes.handlers.IItemListMenuChangeHandler;
import com.lasthopesoftware.bluewater.servers.library.repository.LibrarySession;
import com.lasthopesoftware.bluewater.servers.library.views.handlers.OnGetLibraryViewItemResultsComplete;
import com.vedsoft.fluent.FluentTask;
import com.vedsoft.futures.runnables.TwoParameterRunnable;

import java.util.List;

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

    	LibrarySession.GetActiveLibrary(activity, activeLibrary -> {
		    final TwoParameterRunnable<FluentTask<String, Void, List<Item>>, List<Item>> onGetVisibleViewsCompleteListener = (owner1, result) -> {
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
	    });

        return layout;
    }

	private ListView BuildStandardItemView(final Activity activity, final ViewGroup container, final int position, final IItem category, final View loadingView) {
		final ListView listView = new ListView(activity);
    	listView.setVisibility(View.INVISIBLE);

		final OnGetLibraryViewItemResultsComplete onGetLibraryViewItemResultsComplete = new OnGetLibraryViewItemResultsComplete(activity, container, listView, loadingView, position, itemListMenuChangeHandler);

		ItemProvider
				.provide(SessionConnection.getSessionConnectionProvider(), category.getKey())
				.onComplete(onGetLibraryViewItemResultsComplete)
				.onError(new HandleViewIoException<String, Void, List<Item>>(activity, new Runnable() {

					@Override
					public void run() {
							ItemProvider
								.provide(SessionConnection.getSessionConnectionProvider(), category.getKey())
								.onComplete(onGetLibraryViewItemResultsComplete)
								.onError(new HandleViewIoException<String, Void, List<Item>>(activity, this))
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