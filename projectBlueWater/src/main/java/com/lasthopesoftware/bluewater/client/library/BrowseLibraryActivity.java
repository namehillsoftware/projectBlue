package com.lasthopesoftware.bluewater.client.library;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.ViewAnimator;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.viewpager.widget.ViewPager;

import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.client.connection.HandleViewIoException;
import com.lasthopesoftware.bluewater.client.connection.session.InstantiateSessionConnectionActivity;
import com.lasthopesoftware.bluewater.client.connection.session.ProvideSessionConnection;
import com.lasthopesoftware.bluewater.client.connection.session.SessionConnectionProvider;
import com.lasthopesoftware.bluewater.client.library.access.ISelectedBrowserLibraryProvider;
import com.lasthopesoftware.bluewater.client.library.access.LibraryRepository;
import com.lasthopesoftware.bluewater.client.library.access.SelectedBrowserLibraryProvider;
import com.lasthopesoftware.bluewater.client.library.items.IItem;
import com.lasthopesoftware.bluewater.client.library.items.Item;
import com.lasthopesoftware.bluewater.client.library.items.list.IItemListViewContainer;
import com.lasthopesoftware.bluewater.client.library.items.list.menus.changes.handlers.ItemListMenuChangeHandler;
import com.lasthopesoftware.bluewater.client.library.items.media.files.nowplaying.NowPlayingFloatingActionButton;
import com.lasthopesoftware.bluewater.client.library.items.menu.LongClickViewAnimatorListener;
import com.lasthopesoftware.bluewater.client.library.items.playlists.PlaylistListFragment;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.bluewater.client.library.views.BrowseLibraryViewsFragment;
import com.lasthopesoftware.bluewater.client.library.views.DownloadViewItem;
import com.lasthopesoftware.bluewater.client.library.views.KnownViews;
import com.lasthopesoftware.bluewater.client.library.views.PlaylistViewItem;
import com.lasthopesoftware.bluewater.client.library.views.ViewItem;
import com.lasthopesoftware.bluewater.client.library.views.access.LibraryViewsByConnectionProvider;
import com.lasthopesoftware.bluewater.client.library.views.access.LibraryViewsProvider;
import com.lasthopesoftware.bluewater.client.library.views.access.ProvideLibraryViews;
import com.lasthopesoftware.bluewater.client.library.views.access.ProvideSelectedLibraryView;
import com.lasthopesoftware.bluewater.client.library.views.access.SelectedLibraryViewProvider;
import com.lasthopesoftware.bluewater.client.library.views.adapters.SelectStaticViewAdapter;
import com.lasthopesoftware.bluewater.client.library.views.adapters.SelectViewAdapter;
import com.lasthopesoftware.bluewater.client.servers.selection.BrowserLibrarySelection;
import com.lasthopesoftware.bluewater.client.servers.selection.LibrarySelectionKey;
import com.lasthopesoftware.bluewater.client.servers.selection.SelectedBrowserLibraryIdentifierProvider;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.fragment.ActiveFileDownloadsFragment;
import com.lasthopesoftware.bluewater.settings.ApplicationSettingsActivity;
import com.lasthopesoftware.bluewater.shared.MagicPropertyBuilder;
import com.lasthopesoftware.bluewater.shared.android.view.LazyViewFinder;
import com.lasthopesoftware.bluewater.shared.android.view.ViewUtils;
import com.lasthopesoftware.bluewater.shared.exceptions.UnexpectedExceptionToasterResponse;
import com.lasthopesoftware.bluewater.shared.promises.extensions.LoopedInPromise;
import com.namehillsoftware.handoff.promises.response.VoidResponse;
import com.namehillsoftware.lazyj.AbstractSynchronousLazy;
import com.namehillsoftware.lazyj.CreateAndHold;
import com.namehillsoftware.lazyj.Lazy;

import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static com.lasthopesoftware.bluewater.shared.promises.ForwardedResponse.forward;

public class BrowseLibraryActivity extends AppCompatActivity implements IItemListViewContainer, Runnable {

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

	private final CreateAndHold<ProvideSessionConnection> lazySessionConnectionProvider = new Lazy<>(() -> new SessionConnectionProvider(this));

	private final CreateAndHold<ProvideLibraryViews> lazyLibraryViewsProvider = new AbstractSynchronousLazy<ProvideLibraryViews>() {
		@Override
		protected ProvideLibraryViews create() {
			return new LibraryViewsProvider(
				lazySessionConnectionProvider.getObject(),
				new LibraryViewsByConnectionProvider());
		}
	};

	private final CreateAndHold<ProvideSelectedLibraryView> lazySelectedLibraryViews = new AbstractSynchronousLazy<ProvideSelectedLibraryView>() {
		@Override
		protected ProvideSelectedLibraryView create() {
			return new SelectedLibraryViewProvider(
				lazySelectedBrowserLibraryProvider.getObject(),
				lazyLibraryViewsProvider.getObject(),
				lazyLibraryRepository.getObject());
		}
	};

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

		InstantiateSessionConnectionActivity.restoreSessionConnection(this)
			.eventually(LoopedInPromise.response(new VoidResponse<>(restore -> {
				if (!restore) startLibrary();
			}), this));
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == InstantiateSessionConnectionActivity.ACTIVITY_ID) startLibrary();

		super.onActivityResult(requestCode, resultCode, data);
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
			.eventually(LoopedInPromise.response(new VoidResponse<>(library -> {
				// No library, must bail out
				if (library == null) {
					finish();
					return;
				}

				if (showDownloadsAction.equals(getIntent().getAction())) {
					library.setSelectedView(0);
					library.setSelectedViewType(Library.ViewType.DownloadView);
					lazyLibraryRepository.getObject().saveLibrary(library)
						.eventually(LoopedInPromise.response(new VoidResponse<>(this::displayLibrary), this));

					// Clear the action
					getIntent().setAction(null);
					return;
				}

				displayLibrary(library);
			}), this));
	}

	private void displayLibrary(final Library library) {
		specialLibraryItemsListView.findView().setAdapter(new SelectStaticViewAdapter(this, specialViews, library.getSelectedViewType(), library.getSelectedView()));

		run();
	}

	@Override
	public void run() {
		lazySelectedLibraryViews.getObject()
			.promiseSelectedOrDefaultView()
			.eventually(selectedView -> lazyLibraryViewsProvider.getObject().promiseLibraryViews()
				.eventually(LoopedInPromise.response(
					new VoidResponse<>(items -> updateLibraryView(selectedView, items)),
					this)))
			.excuse(new HandleViewIoException(this, this))
			.excuse(forward())
			.eventually(LoopedInPromise.response(new UnexpectedExceptionToasterResponse(this), this))
			.then(new VoidResponse<>(v -> {
				ApplicationSettingsActivity.launch(this);
				finish();
			}));
	}

	private void updateLibraryView(final ViewItem selectedView, final Collection<ViewItem> items) {
		if (isStopped || items == null) return;

		LongClickViewAnimatorListener.tryFlipToPreviousView(viewAnimator);

		selectViewsListView.findView().setAdapter(new SelectViewAdapter(this, items, selectedView.getKey()));
		selectViewsListView.findView().setOnItemClickListener(getOnSelectViewClickListener(items));

		hideAllViews();
		if (selectedView instanceof DownloadViewItem) {
			oldTitle = specialViews.get(0);
			getSupportActionBar().setTitle(oldTitle);

			final ActiveFileDownloadsFragment activeFileDownloadsFragment = new ActiveFileDownloadsFragment();
			swapFragments(activeFileDownloadsFragment);

			return;
		}

		for (IItem item : items) {
			if (item.getKey() != selectedView.getKey()) continue;
			oldTitle = item.getValue();
			getSupportActionBar().setTitle(oldTitle);
			break;
		}

		if (selectedView instanceof PlaylistViewItem) {
			final PlaylistListFragment playlistListFragment = new PlaylistListFragment();
			playlistListFragment.setOnItemListMenuChangeHandler(new ItemListMenuChangeHandler(this));
			swapFragments(playlistListFragment);

			return;
		}

		final BrowseLibraryViewsFragment browseLibraryViewsFragment = new BrowseLibraryViewsFragment();
		browseLibraryViewsFragment.setOnItemListMenuChangeHandler(new ItemListMenuChangeHandler(this));
		swapFragments(browseLibraryViewsFragment);
	}

	private OnItemClickListener getOnSelectViewClickListener(final Collection<ViewItem> items) {
		return (parent, view, position, id) -> {
			final Item selectedItem = (items instanceof List ? (List<ViewItem>)items : new ArrayList<>(items)).get(position);
			updateSelectedView(KnownViews.Playlists.equals(selectedItem.getValue())
				? Library.ViewType.PlaylistView
				: Library.ViewType.StandardServerView, selectedItem.getKey());
		};
	}

	private void updateSelectedView(final Library.ViewType selectedViewType, final int selectedViewKey) {
		drawerLayout.findView().closeDrawer(GravityCompat.START);
		drawerToggle.getObject().syncState();

		lazySelectedBrowserLibraryProvider.getObject()
			.getBrowserLibrary()
			.then(new VoidResponse<>(library -> {
				if (selectedViewType == library.getSelectedViewType() && library.getSelectedView() == selectedViewKey) return;

				library.setSelectedView(selectedViewKey);
				library.setSelectedViewType(selectedViewType);
				lazyLibraryRepository.getObject().saveLibrary(library)
					.eventually(LoopedInPromise.response(new VoidResponse<>(this::displayLibrary), this));
			}));
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
