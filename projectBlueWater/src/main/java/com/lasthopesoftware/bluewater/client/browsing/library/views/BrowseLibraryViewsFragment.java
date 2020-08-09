package com.lasthopesoftware.bluewater.client.browsing.library.views;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.ViewAnimator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.ViewPager;

import com.astuetz.PagerSlidingTabStrip;
import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.client.browsing.items.Item;
import com.lasthopesoftware.bluewater.client.browsing.items.access.ItemProvider;
import com.lasthopesoftware.bluewater.client.browsing.items.list.menus.changes.handlers.IItemListMenuChangeHandler;
import com.lasthopesoftware.bluewater.client.browsing.items.menu.LongClickViewAnimatorListener;
import com.lasthopesoftware.bluewater.client.browsing.library.access.ILibraryProvider;
import com.lasthopesoftware.bluewater.client.browsing.library.access.LibraryRepository;
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.ISelectedLibraryIdentifierProvider;
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.SelectedBrowserLibraryIdentifierProvider;
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library;
import com.lasthopesoftware.bluewater.client.connection.HandleViewIoException;
import com.lasthopesoftware.bluewater.client.connection.session.SessionConnection;
import com.lasthopesoftware.bluewater.shared.MagicPropertyBuilder;
import com.lasthopesoftware.bluewater.shared.exceptions.UnexpectedExceptionToasterResponse;
import com.lasthopesoftware.bluewater.shared.promises.extensions.LoopedInPromise;
import com.namehillsoftware.handoff.promises.Promise;
import com.namehillsoftware.handoff.promises.response.PromisedResponse;
import com.namehillsoftware.handoff.promises.response.VoidResponse;

import java.util.List;

public class BrowseLibraryViewsFragment extends Fragment implements IItemListMenuChangeHandler {

	private static final String SAVED_TAB_KEY = MagicPropertyBuilder.buildMagicPropertyName(BrowseLibraryViewsFragment.class, "SAVED_TAB_KEY");
	private static final String SAVED_SCROLL_POS = MagicPropertyBuilder.buildMagicPropertyName(BrowseLibraryViewsFragment.class, "SAVED_SCROLL_POS");
	private static final String SAVED_SELECTED_VIEW = MagicPropertyBuilder.buildMagicPropertyName(BrowseLibraryViewsFragment.class, "SAVED_SELECTED_VIEW");

	private ViewAnimator viewAnimator;
	private IItemListMenuChangeHandler itemListMenuChangeHandler;
	private ViewPager viewPager;

	public BrowseLibraryViewsFragment() {
		super(R.layout.tabbed_library_items_layout);
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		viewPager = view.findViewById(R.id.libraryViewPager);
		final RelativeLayout tabbedLibraryViewsContainer = view.findViewById(R.id.tabbedLibraryViewsContainer);
		final PagerSlidingTabStrip libraryViewsTabs = view.findViewById(R.id.tabsLibraryViews);
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

		final ProgressBar loadingView = view.findViewById(R.id.pbLoadingTabbedItems);

		tabbedLibraryViewsContainer.setVisibility(View.INVISIBLE);
		loadingView.setVisibility(View.VISIBLE);

		final Context context = getContext();
		if (context == null) return;

		final Handler handler = new Handler(context.getMainLooper());

		final PromisedResponse<List<Item>, Void> onGetVisibleViewsCompleteListener =
			LoopedInPromise.response((result) -> {
				if (result == null) return null;

				final LibraryViewPagerAdapter viewChildPagerAdapter = new LibraryViewPagerAdapter(getChildFragmentManager());
				viewChildPagerAdapter.setOnItemListMenuChangeHandler(BrowseLibraryViewsFragment.this);

				viewChildPagerAdapter.setLibraryViews(result);

				// Set up the ViewPager with the sections adapter.
				viewPager.setAdapter(viewChildPagerAdapter);
				libraryViewsTabs.setViewPager(viewPager);

				libraryViewsTabs.setVisibility(result.size() <= 1 ? View.GONE : View.VISIBLE);

				loadingView.setVisibility(View.INVISIBLE);
				tabbedLibraryViewsContainer.setVisibility(View.VISIBLE);

				return null;
			}, handler);

		final Runnable fillItemsAction = new Runnable() {
			@Override
			public void run() {
				getSelectedBrowserLibrary()
					.then(activeLibrary ->
						SessionConnection.getInstance(context).promiseSessionConnection()
							.eventually(c -> ItemProvider.provide(c, activeLibrary.getSelectedView()))
							.eventually(onGetVisibleViewsCompleteListener)
							.eventually(LoopedInPromise.response(new VoidResponse<>(v -> {
								if (savedInstanceState == null) return;

								final int savedSelectedView = savedInstanceState.getInt(SAVED_SELECTED_VIEW, -1);
								if (savedSelectedView < 0 || savedSelectedView != activeLibrary.getSelectedView()) return;

								final int savedTabKey = savedInstanceState.getInt(SAVED_TAB_KEY, -1);
								if (savedTabKey > -1)
									viewPager.setCurrentItem(savedTabKey);

								final int savedScrollPosition = savedInstanceState.getInt(SAVED_SCROLL_POS, -1);
								if (savedScrollPosition > -1)
									viewPager.setScrollY(savedScrollPosition);
							}), handler))
							.excuse(new HandleViewIoException(context, this))
							.eventuallyExcuse(LoopedInPromise.response(new UnexpectedExceptionToasterResponse(context), handler)));
			}
		};

		fillItemsAction.run();
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
	public void onSaveInstanceState(@NonNull final Bundle outState) {
		super.onSaveInstanceState(outState);

		if (viewPager == null) return;

		outState.putInt(SAVED_TAB_KEY, viewPager.getCurrentItem());
		outState.putInt(SAVED_SCROLL_POS, viewPager.getScrollY());

		getSelectedBrowserLibrary()
			.then(new VoidResponse<>(library -> {
				if (library != null)
					outState.putInt(SAVED_SELECTED_VIEW, library.getSelectedView());
			}));
	}

	private Promise<Library> getSelectedBrowserLibrary() {
		final Context context = getContext();
		if (context == null) return Promise.empty();

		final ISelectedLibraryIdentifierProvider selectedLibraryIdentifierProvider = new SelectedBrowserLibraryIdentifierProvider(context);
		final ILibraryProvider libraryProvider = new LibraryRepository(context);

		return libraryProvider.getLibrary(selectedLibraryIdentifierProvider.getSelectedLibraryId());
	}
}
