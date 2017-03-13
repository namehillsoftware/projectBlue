package com.lasthopesoftware.bluewater.client.library;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.ViewAnimator;

import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.client.connection.HandleViewIoException;
import com.lasthopesoftware.bluewater.client.connection.InstantiateSessionConnectionActivity;
import com.lasthopesoftware.bluewater.client.connection.SessionConnection;
import com.lasthopesoftware.bluewater.client.library.access.ISelectedBrowserLibraryProvider;
import com.lasthopesoftware.bluewater.client.library.access.LibraryRepository;
import com.lasthopesoftware.bluewater.client.library.access.LibraryViewsProvider;
import com.lasthopesoftware.bluewater.client.library.access.SelectedBrowserLibraryProvider;
import com.lasthopesoftware.bluewater.client.library.items.IItem;
import com.lasthopesoftware.bluewater.client.library.items.Item;
import com.lasthopesoftware.bluewater.client.library.items.list.IItemListViewContainer;
import com.lasthopesoftware.bluewater.client.library.items.list.menus.changes.handlers.ItemListMenuChangeHandler;
import com.lasthopesoftware.bluewater.client.library.items.media.files.nowplaying.NowPlayingFloatingActionButton;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.fragment.ActiveFileDownloadsFragment;
import com.lasthopesoftware.bluewater.client.library.items.menu.LongClickViewAnimatorListener;
import com.lasthopesoftware.bluewater.client.library.items.playlists.PlaylistListFragment;
import com.lasthopesoftware.bluewater.client.library.items.playlists.access.PlaylistsProvider;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.bluewater.client.library.views.BrowseLibraryViewsFragment;
import com.lasthopesoftware.bluewater.client.library.views.adapters.SelectStaticViewAdapter;
import com.lasthopesoftware.bluewater.client.library.views.adapters.SelectViewAdapter;
import com.lasthopesoftware.bluewater.client.servers.selection.SelectedBrowserLibraryIdentifierProvider;
import com.lasthopesoftware.bluewater.shared.MagicPropertyBuilder;
import com.lasthopesoftware.bluewater.shared.promises.resolutions.Dispatch;
import com.lasthopesoftware.bluewater.shared.view.LazyViewFinder;
import com.lasthopesoftware.bluewater.shared.view.ViewUtils;
import com.vedsoft.futures.callables.VoidFunc;
import com.vedsoft.futures.runnables.OneParameterAction;
import com.vedsoft.lazyj.AbstractThreadLocalLazy;
import com.vedsoft.lazyj.ILazy;

import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;

public class BrowseLibraryActivity extends AppCompatActivity implements IItemListViewContainer {

	public static final String showDownloadsAction = MagicPropertyBuilder.buildMagicPropertyName(BrowseLibraryActivity.class, "showDownloadsAction");

	private static final List<String> specialViews = Collections.singletonList("Active Downloads");

	/**
	 * The {@link ViewPager} that will host the section contents.
	 */
	private final LazyViewFinder<RelativeLayout> browseLibraryContainerRelativeLayout = new LazyViewFinder<>(this, R.id.browseLibraryContainer);
	private final LazyViewFinder<ListView> selectViewsListView = new LazyViewFinder<>(this, R.id.lvLibraryViewSelection);
	private final LazyViewFinder<ListView> specialLibraryItemsListView = new LazyViewFinder<>(this, R.id.specialLibraryItemsListView);
	private final LazyViewFinder<DrawerLayout> drawerLayout = new LazyViewFinder<>(this, R.id.drawer_layout);
	private final LazyViewFinder<ProgressBar> loadingViewsProgressBar = new LazyViewFinder<>(this, R.id.pbLoadingViews);
	private final ILazy<LibraryRepository> lazyLibraryRepository = new AbstractThreadLocalLazy<LibraryRepository>() {
		@Override
		protected LibraryRepository initialize() throws Exception {
			return new LibraryRepository(BrowseLibraryActivity.this);
		}
	};

	private ILazy<ISelectedBrowserLibraryProvider> lazySelectedBrowserLibraryProvider = new AbstractThreadLocalLazy<ISelectedBrowserLibraryProvider>() {
		@Override
		protected ISelectedBrowserLibraryProvider initialize() throws Exception {
			return new SelectedBrowserLibraryProvider(
				new SelectedBrowserLibraryIdentifierProvider(BrowseLibraryActivity.this),
				lazyLibraryRepository.getObject());
		}
	};

	private ViewAnimator viewAnimator;
	private NowPlayingFloatingActionButton nowPlayingFloatingActionButton;

	private Fragment activeFragment;

	private ActionBarDrawerToggle drawerToggle = null;

	private CharSequence oldTitle;
	private boolean isStopped = false;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

        // Ensure that this task is only started when it's the task root. A workaround for an Android bug.
        // See http://stackoverflow.com/a/7748416
        if (!isTaskRoot()) {
            final Intent intent = getIntent();
	        if (Intent.ACTION_MAIN.equals(intent.getAction()) && intent.hasCategory(Intent.CATEGORY_LAUNCHER)) {
		        final String className = BrowseLibraryActivity.class.getName();
		        LoggerFactory.getLogger(getClass()).info(className + " is not the root.  Finishing " + className + " instead of launching.");
		        finish();
		        return;
	        }
        }

		setContentView(R.layout.activity_browse_library);

		nowPlayingFloatingActionButton = NowPlayingFloatingActionButton.addNowPlayingFloatingActionButton((RelativeLayout) findViewById(R.id.browseLibraryRelativeLayout));

		setTitle(R.string.title_activity_library);

		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		getSupportActionBar().setHomeButtonEnabled(true);

		final DrawerLayout drawerLayout = this.drawerLayout.findView();
		drawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);

		oldTitle = getTitle();
		final CharSequence selectViewTitle = getText(R.string.select_view_title);
		drawerToggle = new ActionBarDrawerToggle(
                this,                  /* host Activity */
				drawerLayout,         /* DrawerLayout object */
                R.string.drawer_open,  /* "open drawer" description */
                R.string.drawer_close  /* "close drawer" description */
		) {
			 /** Called when a drawer has settled in a completely closed state. */
			@Override
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
				getSupportActionBar().setTitle(oldTitle);
                invalidateOptionsMenu(); // creates resultFrom to onPrepareOptionsMenu()
            }

            /** Called when a drawer has settled in a completely open state. */
			@Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                oldTitle = getSupportActionBar().getTitle();
				getSupportActionBar().setTitle(selectViewTitle);
                invalidateOptionsMenu(); // creates resultFrom to onPrepareOptionsMenu()
            }

		};

		drawerLayout.setDrawerListener(drawerToggle);

		specialLibraryItemsListView.findView().setOnItemClickListener((parent, view, position, id) -> updateSelectedView(Library.ViewType.DownloadView, 0));
	}

	@Override
	public void onStart() {
		super.onStart();

		if (!InstantiateSessionConnectionActivity.restoreSessionConnection(this)) startLibrary();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == InstantiateSessionConnectionActivity.ACTIVITY_ID) startLibrary();
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);

		if (showDownloadsAction.equals(intent.getAction()))
			updateSelectedView(Library.ViewType.DownloadView, 0);
	}

	private void startLibrary() {
		isStopped = false;
		if (selectViewsListView.findView().getAdapter() != null) return;

        showProgressBar();

		lazySelectedBrowserLibraryProvider.getObject()
			.getBrowserLibrary()
			.then(Dispatch.toContext(VoidFunc.runCarelessly(library -> {
				// No library, must bail out
				if (library == null) {
					finish();
					return;
				}

				if (showDownloadsAction.equals(getIntent().getAction())) {
					library.setSelectedView(0);
					library.setSelectedViewType(Library.ViewType.DownloadView);
					lazyLibraryRepository.getObject().saveLibrary(library);

					// Clear the action
					getIntent().setAction(null);
				}

				displayLibrary(library);
			}), this));
	}

	private void displayLibrary(final Library library) {
		final Library.ViewType selectedViewType = library.getSelectedViewType();

		specialLibraryItemsListView.findView().setAdapter(new SelectStaticViewAdapter(this, specialViews, selectedViewType, library.getSelectedView()));

		OneParameterAction<List<Item>> onCompleteAction =
			(items) -> {
				if (isStopped || items == null) return;

				LongClickViewAnimatorListener.tryFlipToPreviousView(viewAnimator);

				selectViewsListView.findView().setAdapter(new SelectViewAdapter(BrowseLibraryActivity.this, items, selectedViewType, library.getSelectedView()));
				selectViewsListView.findView().setOnItemClickListener(getOnSelectViewClickListener(items));

				hideAllViews();
				if (!Library.ViewType.serverViewTypes.contains(selectedViewType)) {
					oldTitle = specialViews.get(0);
					getSupportActionBar().setTitle(oldTitle);

					final ActiveFileDownloadsFragment activeFileDownloadsFragment = new ActiveFileDownloadsFragment();
					swapFragments(activeFileDownloadsFragment);

					return;
				}

				for (IItem item : items) {
					if (item.getKey() != library.getSelectedView()) continue;
					oldTitle = item.getValue();
					getSupportActionBar().setTitle(oldTitle);
					break;
				}

				if (selectedViewType == Library.ViewType.PlaylistView) {
					final PlaylistListFragment playlistListFragment = new PlaylistListFragment();
					playlistListFragment.setOnItemListMenuChangeHandler(new ItemListMenuChangeHandler(BrowseLibraryActivity.this));
					swapFragments(playlistListFragment);

					return;
				}

				final BrowseLibraryViewsFragment browseLibraryViewsFragment = new BrowseLibraryViewsFragment();
				browseLibraryViewsFragment.setOnItemListMenuChangeHandler(new ItemListMenuChangeHandler(BrowseLibraryActivity.this));
				swapFragments(browseLibraryViewsFragment);
			};

		final Runnable getLibraryViewsRunnable = new Runnable() {
			@Override
			public void run() {
				new LibraryViewsProvider(SessionConnection.getSessionConnectionProvider())
					.onComplete(onCompleteAction)
					.onError(new HandleViewIoException<>(BrowseLibraryActivity.this, this))
					.execute();
			}
		};

		getLibraryViewsRunnable.run();
	}

	private OnItemClickListener getOnSelectViewClickListener(final List<Item> items) {
		return (parent, view, position, id) -> {
			final Item selectedItem = items.get(position);
			updateSelectedView(PlaylistsProvider.PlaylistsItemKey.equals(selectedItem.getValue()) ? Library.ViewType.PlaylistView : Library.ViewType.StandardServerView, selectedItem.getKey());
		};
	}

	private void updateSelectedView(final Library.ViewType selectedViewType, final int selectedViewKey) {
		drawerLayout.findView().closeDrawer(GravityCompat.START);
		drawerToggle.syncState();

		lazySelectedBrowserLibraryProvider.getObject()
			.getBrowserLibrary()
			.then(Dispatch.toContext(VoidFunc.runCarelessly(library -> {
				if (selectedViewType == library.getSelectedViewType() && library.getSelectedView() == selectedViewKey) return;

				library.setSelectedView(selectedViewKey);
				library.setSelectedViewType(selectedViewType);
				lazyLibraryRepository.getObject().saveLibrary(library);

				displayLibrary(library);
			}), this));
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		return ViewUtils.buildStandardMenu(this, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		return drawerToggle != null && drawerToggle.onOptionsItemSelected(item) || ViewUtils.handleMenuClicks(this, item);
	}

	@Override
    protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        if (drawerToggle != null) drawerToggle.syncState();
    }

	@Override
    public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
        if (drawerToggle != null) drawerToggle.onConfigurationChanged(newConfig);
    }

	private void showProgressBar() {
		showContainerView(loadingViewsProgressBar.findView());
	}

	private void showContainerView(View view) {
		hideAllViews();
		view.setVisibility(View.VISIBLE);
	}

	private void hideAllViews() {
		for (int i = 0; i < browseLibraryContainerRelativeLayout.findView().getChildCount(); i++)
			browseLibraryContainerRelativeLayout.findView().getChildAt(i).setVisibility(View.INVISIBLE);
	}

	private synchronized void swapFragments(Fragment newFragment) {
		final FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
		try {
			if (activeFragment != null)
				ft.remove(activeFragment);

			ft.add(R.id.browseLibraryContainer, newFragment);
		} finally {
			ft.commit();
			activeFragment = newFragment;
		}
	}
	
	@Override
	public void onStop() {
		isStopped = true;
		super.onStop();
	}

	@Override
    public void onBackPressed() {
        if (LongClickViewAnimatorListener.tryFlipToPreviousView(viewAnimator)) return;

        super.onBackPressed();
    }

	@Override
	public void updateViewAnimator(ViewAnimator viewAnimator) {
		this.viewAnimator = viewAnimator;
	}

	@Override
	public NowPlayingFloatingActionButton getNowPlayingFloatingActionButton() {
		return nowPlayingFloatingActionButton;
	}
}
