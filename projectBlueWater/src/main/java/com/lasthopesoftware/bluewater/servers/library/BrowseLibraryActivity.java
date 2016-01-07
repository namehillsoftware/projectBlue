package com.lasthopesoftware.bluewater.servers.library;

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
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.ViewAnimator;

import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.servers.connection.HandleViewIoException;
import com.lasthopesoftware.bluewater.servers.connection.InstantiateSessionConnectionActivity;
import com.lasthopesoftware.bluewater.servers.connection.SessionConnection;
import com.lasthopesoftware.bluewater.servers.library.access.LibraryViewsProvider;
import com.lasthopesoftware.bluewater.servers.library.items.IItem;
import com.lasthopesoftware.bluewater.servers.library.items.Item;
import com.lasthopesoftware.bluewater.servers.library.items.list.IItemListViewContainer;
import com.lasthopesoftware.bluewater.servers.library.items.list.menus.changes.handlers.ItemListMenuChangeHandler;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.nowplaying.NowPlayingFloatingActionButton;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.stored.fragment.ActiveFileDownloadsFragment;
import com.lasthopesoftware.bluewater.servers.library.items.menu.LongClickViewAnimatorListener;
import com.lasthopesoftware.bluewater.servers.library.items.playlists.PlaylistListFragment;
import com.lasthopesoftware.bluewater.servers.library.items.playlists.access.PlaylistsProvider;
import com.lasthopesoftware.bluewater.servers.library.repository.Library;
import com.lasthopesoftware.bluewater.servers.library.repository.LibrarySession;
import com.lasthopesoftware.bluewater.servers.library.views.BrowseLibraryViewsFragment;
import com.lasthopesoftware.bluewater.servers.library.views.adapters.SelectStaticViewAdapter;
import com.lasthopesoftware.bluewater.servers.library.views.adapters.SelectViewAdapter;
import com.lasthopesoftware.bluewater.shared.SpecialValueHelpers;
import com.lasthopesoftware.bluewater.shared.view.ViewUtils;
import com.vedsoft.fluent.FluentTask;
import com.vedsoft.futures.runnables.TwoParameterRunnable;

import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;

public class BrowseLibraryActivity extends AppCompatActivity implements IItemListViewContainer {

	public static final String showDownloadsAction = SpecialValueHelpers.buildMagicPropertyName(BrowseLibraryActivity.class, "showDownloadsAction");

	private static final String className = BrowseLibraryActivity.class.getName();

	private static final List<String> specialViews = Collections.singletonList("Active Downloads");

	/**
	 * The {@link ViewPager} that will host the section contents.
	 */
	private RelativeLayout browseLibraryContainerRelativeLayout;
	private ListView selectViewsListView;
	private ListView specialLibraryItemsListView;
	private DrawerLayout drawerLayout;
	private ProgressBar loadingViewsProgressBar;

	private ViewAnimator viewAnimator;
	private NowPlayingFloatingActionButton nowPlayingFloatingActionButton;

	private Fragment activeFragment;

	private ActionBarDrawerToggle drawerToggle = null;

	private CharSequence oldTitle;
	private boolean isStopped = false;

	private boolean isLibraryChanged = false;

	private Intent newIntent;

	private final BroadcastReceiver onLibraryChanged = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			isLibraryChanged = true;
		}
	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

        // Ensure that this task is only started when it's the task root. A workaround for an Android bug.
        // See http://stackoverflow.com/a/7748416
        if (!isTaskRoot()) {
            final Intent intent = getIntent();
            final String intentAction = intent.getAction();

	        if (intentAction != null && intentAction.equals(Intent.ACTION_MAIN) && intent.hasCategory(Intent.CATEGORY_LAUNCHER)) {
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

		drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
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
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }

            /** Called when a drawer has settled in a completely open state. */
			@Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                oldTitle = getSupportActionBar().getTitle();
				getSupportActionBar().setTitle(selectViewTitle);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }

		};

		drawerLayout.setDrawerListener(drawerToggle);
		selectViewsListView = (ListView) findViewById(R.id.lvLibraryViewSelection);

		loadingViewsProgressBar = (ProgressBar) findViewById(R.id.pbLoadingViews);
		browseLibraryContainerRelativeLayout = (RelativeLayout) findViewById(R.id.browseLibraryContainer);

		LocalBroadcastManager.getInstance(this).registerReceiver(onLibraryChanged, new IntentFilter(LibrarySession.libraryChosenEvent));

		specialLibraryItemsListView = (ListView) findViewById(R.id.specialLibraryItemsListView);
		specialLibraryItemsListView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				updateSelectedView(Library.ViewType.DownloadView, 0);
			}
		});
	}

	@Override
	public void onStart() {
		super.onStart();

		if (isLibraryChanged) {
			startActivity(new Intent(this, InstantiateSessionConnectionActivity.class));
			finish();
			return;
		}

		if (!InstantiateSessionConnectionActivity.restoreSessionConnection(this)) startLibrary();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == InstantiateSessionConnectionActivity.ACTIVITY_ID) startLibrary();
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);

		newIntent = intent;
	}

	private void startLibrary() {
		isStopped = false;
		if (selectViewsListView.getAdapter() != null) return;

        showProgressBar();

		LibrarySession.GetActiveLibrary(this, new TwoParameterRunnable<FluentTask<Integer, Void, Library>, Library>() {

			@Override
			public void run(FluentTask<Integer, Void, Library> owner, final Library library) {
				// No library, must bail out
				if (library == null) {
					finish();
					return;
				}

				if (newIntent != null) {

					if (showDownloadsAction.equals(newIntent.getAction())) {
						library.setSelectedView(0);
						library.setSelectedViewType(Library.ViewType.DownloadView);
						LibrarySession.SaveLibrary(BrowseLibraryActivity.this, library);
					}

					// Now that newIntent has been handled, set it to null
					newIntent = null;
				}

				displayLibrary(library);
			}
		});

	}

	private void displayLibrary(final Library library) {
		final Library.ViewType selectedViewType = library.getSelectedViewType();

		specialLibraryItemsListView.setAdapter(new SelectStaticViewAdapter(this, specialViews, selectedViewType, library.getSelectedView()));

		new LibraryViewsProvider(SessionConnection.getSessionConnectionProvider())
				.onComplete(new TwoParameterRunnable<FluentTask<String,Void,List<Item>>, List<Item>>() {

			        @Override
			        public void run(FluentTask<String, Void, List<Item>> owner, final List<Item> items) {
				        if (isStopped || items == null) return;

				        LongClickViewAnimatorListener.tryFlipToPreviousView(viewAnimator);

				        selectViewsListView.setAdapter(new SelectViewAdapter(BrowseLibraryActivity.this, items, selectedViewType, library.getSelectedView()));
				        selectViewsListView.setOnItemClickListener(getOnSelectViewClickListener(items));

				        hideAllViews();
				        if (!Library.serverViewTypes.contains(selectedViewType)) {
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
					}
				})
				.onError(new HandleViewIoException<String, Void, List<Item>>(this, new Runnable() {

					@Override
					public void run() {
						// Get a new instance of the file system as the connection provider may have changed
						displayLibrary(library);
					}
				}))
				.execute();
	}

	private OnItemClickListener getOnSelectViewClickListener(final List<Item> items) {
		return new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				final Item selectedItem = items.get(position);

				// The action state can be preserved due to the need to preserve action state
				getIntent().setAction(null);

				updateSelectedView(PlaylistsProvider.PlaylistsItemKey.equals(selectedItem.getValue()) ? Library.ViewType.PlaylistView : Library.ViewType.StandardServerView, selectedItem.getKey());
			}
		};
	}

	private void updateSelectedView(final Library.ViewType selectedViewType, final int selectedViewKey) {
		drawerLayout.closeDrawer(GravityCompat.START);
		drawerToggle.syncState();

		LibrarySession.GetActiveLibrary(this, new TwoParameterRunnable<FluentTask<Integer,Void,Library>, Library>() {

			@Override
			public void run(FluentTask<Integer, Void, Library> owner, final Library library) {
				if (selectedViewType == library.getSelectedViewType() && library.getSelectedView() == selectedViewKey) return;

				library.setSelectedView(selectedViewKey);
				library.setSelectedViewType(selectedViewType);
				LibrarySession.SaveLibrary(BrowseLibraryActivity.this, library);

				displayLibrary(library);
			}
		});
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
		showContainerView(loadingViewsProgressBar);
	}

	private void showContainerView(View view) {
		hideAllViews();
		view.setVisibility(View.VISIBLE);
	}

	private void hideAllViews() {
		for (int i = 0; i < browseLibraryContainerRelativeLayout.getChildCount(); i++)
			browseLibraryContainerRelativeLayout.getChildAt(i).setVisibility(View.INVISIBLE);
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
	protected void onDestroy() {
		super.onDestroy();

		LocalBroadcastManager.getInstance(this).unregisterReceiver(onLibraryChanged);
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
