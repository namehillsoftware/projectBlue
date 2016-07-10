package com.lasthopesoftware.bluewater.client.library.views;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.ViewAnimator;

import com.astuetz.PagerSlidingTabStrip;
import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.client.connection.HandleViewIoException;
import com.lasthopesoftware.bluewater.client.connection.SessionConnection;
import com.lasthopesoftware.bluewater.client.library.items.Item;
import com.lasthopesoftware.bluewater.client.library.items.access.ItemProvider;
import com.lasthopesoftware.bluewater.client.library.items.list.menus.changes.handlers.IItemListMenuChangeHandler;
import com.lasthopesoftware.bluewater.client.library.items.menu.LongClickViewAnimatorListener;
import com.lasthopesoftware.bluewater.client.library.repository.LibrarySession;
import com.lasthopesoftware.bluewater.shared.MagicPropertyBuilder;
import com.vedsoft.fluent.FluentTask;
import com.vedsoft.futures.runnables.TwoParameterRunnable;

import java.util.List;

/**
 * Created by david on 1/5/16.
 */
public class BrowseLibraryViewsFragment extends Fragment implements IItemListMenuChangeHandler {

	private static final String SAVED_TAB_KEY = MagicPropertyBuilder.buildMagicPropertyName(BrowseLibraryViewsFragment.class, "SAVED_TAB_KEY");
	private static final String SAVED_SCROLL_POS = MagicPropertyBuilder.buildMagicPropertyName(BrowseLibraryViewsFragment.class, "SAVED_SCROLL_POS");
	private static final String SAVED_SELECTED_VIEW = MagicPropertyBuilder.buildMagicPropertyName(BrowseLibraryViewsFragment.class, "SAVED_SELECTED_VIEW");

	private ViewAnimator viewAnimator;
	private IItemListMenuChangeHandler itemListMenuChangeHandler;
	private ViewPager viewPager;

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		final RelativeLayout tabbedItemsLayout = (RelativeLayout) inflater.inflate(R.layout.tabbed_library_items_layout, container, false);

		viewPager = (ViewPager) tabbedItemsLayout.findViewById(R.id.libraryViewPager);
		final RelativeLayout tabbedLibraryViewsContainer = (RelativeLayout) tabbedItemsLayout.findViewById(R.id.tabbedLibraryViewsContainer);
		final PagerSlidingTabStrip libraryViewsTabs = (PagerSlidingTabStrip) tabbedItemsLayout.findViewById(R.id.tabsLibraryViews);
		libraryViewsTabs.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
			@Override
			public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

			}

			@Override
			public void onPageSelected(int position) {
				LongClickViewAnimatorListener.tryFlipToPreviousView(viewAnimator);
			}

			@Override
			public void onPageScrollStateChanged(int state) {

			}
		});

		final ProgressBar loadingView = (ProgressBar) tabbedItemsLayout.findViewById(R.id.pbLoadingTabbedItems);

		tabbedLibraryViewsContainer.setVisibility(View.INVISIBLE);
		loadingView.setVisibility(View.VISIBLE);

		final TwoParameterRunnable<FluentTask<String, Void, List<Item>>, List<Item>> onGetVisibleViewsCompleteListener =  new TwoParameterRunnable<FluentTask<String, Void, List<Item>>, List<Item>>() {

			@Override
			public void run(FluentTask<String, Void, List<Item>> owner, List<Item> result) {
				if (result == null) return;

				final LibraryViewPagerAdapter viewChildPagerAdapter = new LibraryViewPagerAdapter(getChildFragmentManager());
				viewChildPagerAdapter.setOnItemListMenuChangeHandler(BrowseLibraryViewsFragment.this);

				viewChildPagerAdapter.setLibraryViews(result);

				// Set up the ViewPager with the sections adapter.
				viewPager.setAdapter(viewChildPagerAdapter);
				libraryViewsTabs.setViewPager(viewPager);

				libraryViewsTabs.setVisibility(result.size() <= 1 ? View.GONE : View.VISIBLE);

				loadingView.setVisibility(View.INVISIBLE);
				tabbedLibraryViewsContainer.setVisibility(View.VISIBLE);
			}
		};

		LibrarySession.GetActiveLibrary(getContext(), activeLibrary ->
				ItemProvider
					.provide(SessionConnection.getSessionConnectionProvider(), activeLibrary.getSelectedView())
					.onComplete(onGetVisibleViewsCompleteListener)
					.onError(new HandleViewIoException<>(getContext(), new Runnable() {

						@Override
						public void run() {
							ItemProvider
									.provide(SessionConnection.getSessionConnectionProvider(), activeLibrary.getSelectedView())
									.onComplete(onGetVisibleViewsCompleteListener)
									.onError(new HandleViewIoException<>(getContext(), this))
									.execute();
						}
					}))
					.execute()
		);


		return tabbedItemsLayout;
	}


	public void setOnItemListMenuChangeHandler(IItemListMenuChangeHandler itemListMenuChangeHandler) {
		this.itemListMenuChangeHandler = itemListMenuChangeHandler;
	}

	@Override
	public void onAllMenusHidden() {
		if (itemListMenuChangeHandler != null)
			itemListMenuChangeHandler.onAllMenusHidden();
	}

	@Override
	public void onAnyMenuShown() {
		if (itemListMenuChangeHandler != null)
			itemListMenuChangeHandler.onAnyMenuShown();
	}

	@Override
	public void onViewChanged(ViewAnimator viewAnimator) {
		BrowseLibraryViewsFragment.this.viewAnimator = viewAnimator;

		if (itemListMenuChangeHandler != null)
			itemListMenuChangeHandler.onViewChanged(viewAnimator);
	}

	@Override
	public void onSaveInstanceState(final Bundle outState) {
		super.onSaveInstanceState(outState);

		if (viewPager == null) return;

		outState.putInt(SAVED_TAB_KEY, viewPager.getCurrentItem());
		outState.putInt(SAVED_SCROLL_POS, viewPager.getScrollY());
		LibrarySession.GetActiveLibrary(getContext(), library -> {
			if (library != null)
				outState.putInt(SAVED_SELECTED_VIEW, library.getSelectedView());
		});
	}

	@Override
	public void onViewStateRestored(@Nullable final Bundle savedInstanceState) {
		super.onViewStateRestored(savedInstanceState);

		if (savedInstanceState == null || viewPager == null) return;

		LibrarySession.GetActiveLibrary(getContext(), library -> {
			final int savedSelectedView = savedInstanceState.getInt(SAVED_SELECTED_VIEW, -1);
			if (savedSelectedView < 0 || savedSelectedView != library.getSelectedView()) return;

			final int savedTabKey = savedInstanceState.getInt(SAVED_TAB_KEY, -1);
			if (savedTabKey > -1)
				viewPager.setCurrentItem(savedTabKey);

			final int savedScrollPosition = savedInstanceState.getInt(SAVED_SCROLL_POS, -1);
			if (savedScrollPosition > -1)
				viewPager.setScrollY(savedScrollPosition);
		});
	}
}
