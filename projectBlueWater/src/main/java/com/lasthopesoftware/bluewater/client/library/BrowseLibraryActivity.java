package com.lasthopesoftware.bluewater.client.library;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.LocalBroadcastManager;
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
import com.lasthopesoftware.bluewater.client.connection.session.InstantiateSessionConnectionActivity;
import com.lasthopesoftware.bluewater.client.connection.session.SessionConnection;
import com.lasthopesoftware.bluewater.client.library.access.ISelectedBrowserLibraryProvider;
import com.lasthopesoftware.bluewater.client.library.access.LibraryRepository;
import com.lasthopesoftware.bluewater.client.library.access.SelectedBrowserLibraryProvider;
import com.lasthopesoftware.bluewater.client.library.access.views.LibraryViewsProvider;
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
import com.lasthopesoftware.bluewater.client.servers.selection.BrowserLibrarySelection;
import com.lasthopesoftware.bluewater.client.servers.selection.LibrarySelectionKey;
import com.lasthopesoftware.bluewater.client.servers.selection.SelectedBrowserLibraryIdentifierProvider;
import com.lasthopesoftware.bluewater.shared.MagicPropertyBuilder;
import com.lasthopesoftware.bluewater.shared.android.view.LazyViewFinder;
import com.lasthopesoftware.bluewater.shared.android.view.ViewUtils;
import com.lasthopesoftware.bluewater.shared.promises.extensions.LoopedInPromise;
import com.namehillsoftware.handoff.promises.response.PromisedResponse;
import com.namehillsoftware.lazyj.AbstractSynchronousLazy;
import com.namehillsoftware.lazyj.CreateAndHold;
import com.namehillsoftware.lazyj.Lazy;

import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;

import static com.namehillsoftware.handoff.promises.response.ImmediateAction.perform;

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
	private final CreateAndHold<LibraryRepository> lazyLibraryRepository = new AbstractSynchronousLazy<LibraryRepository>() {
		@Override
		protected LibraryRepository create() {
			return new LibraryRepository(BrowseLibraryActivity.this);
		}
	};

	private final CreateAndHold<ActionBarDrawerToggle> drawerToggle = new AbstractSynchronousLazy<ActionBarDrawerToggle>() {
		@Override
		protected ActionBarDrawerToggle create() {
			final CharSequence selectViewTitle = getText(R.string.select_view_title);
			return new ActionBarDrawerToggle(
				BrowseLibraryActivity.this,                  /* host Activity */
				drawerLayout.findView(),         /* DrawerLayout object */
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
		}
	};

	private final CreateAndHold<ISelectedBrowserLibraryProvider> lazySelectedBrowserLibraryProvider = new AbstractSynchronousLazy<ISelectedBrowserLibraryProvider>() {
		@Override
		protected ISelectedBrowserLibraryProvider create() {
			return new SelectedBrowserLibraryProvider(
				new SelectedBrowserLibraryIdentifierProvider(BrowseLibraryActivity.this),
				lazyLibraryRepository.getObject());
		}
	};

	private final BroadcastReceiver libraryChosenEventReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			final int chosenLibrary = intent.getIntExtra(LibrarySelectionKey.chosenLibraryKey, -1);
			if (chosenLibrary >= 0)
				finishAffinity();
		}
	};

	private final CreateAndHold<LocalBroadcastManager> lazyLocalBroadcastManager = new Lazy<>(() -> LocalBroadcastManager.getInstance(this));

	private ViewAnimator viewAnimator;
	private NowPlayingFloatingActionButton nowPlayingFloatingActionButton;

	private Fragment activeFragment;

	private CharSequence oldTitle = getTitle();
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

		lazyLocalBroadcastManager.getObject().registerReceiver(
			libraryChosenEventReceiver,
			new IntentFilter(BrowserLibrarySelection.libraryChosenEvent));

		nowPlayingFloatingActionButton = NowPlayingFloatingActionButton.addNowPlayingFloatingActionButton(findViewById(R.id.browseLibraryRelativeLayout));

		setTitle(R.string.title_activity_library);

		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		getSupportActionBar().setHomeButtonEnabled(true);

		final DrawerLayout drawerLayout = this.drawerLayout.findView();
		drawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);

		drawerLayout.addDrawerListener(drawerToggle.getObject());

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
			.eventually(LoopedInPromise.response(perform(library -> {
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

		PromisedResponse<List<Item>, Void> onCompleteAction =
			LoopedInPromise.response(perform(items -> {
				if (isStopped || items == null) return;

				LongClickViewAnimatorListener.tryFlipToPreviousView(viewAnimator);

				selectViewsListView.findView().setAdapter(new SelectViewAdapter(this, items, selectedViewType, library.getSelectedView()));
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
			}), this);

		final Runnable getLibraryViewsRunnable = new Runnable() {
			@Override
			public void run() {
				LibraryViewsProvider.provide(SessionConnection.getSessionConnectionProvider())
					.eventually(onCompleteAction)
					.excuse(new HandleViewIoException(BrowseLibraryActivity.this, this));
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
		drawerToggle.getObject().syncState();

		lazySelectedBrowserLibraryProvider.getObject()
			.getBrowserLibrary()
			.eventually(LoopedInPromise.response(perform(library -> {
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
		return drawerToggle.isCreated() && drawerToggle.getObject().onOptionsItemSelected(item) || ViewUtils.handleMenuClicks(this, item);
	}

	@Override
    protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        if (drawerToggle.isCreated()) drawerToggle.getObject().syncState();
    }

	@Override
    public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
        if (drawerToggle.isCreated()) drawerToggle.getObject().onConfigurationChanged(newConfig);
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
