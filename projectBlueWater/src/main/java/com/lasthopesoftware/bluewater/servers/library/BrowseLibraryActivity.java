package com.lasthopesoftware.bluewater.servers.library;

import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
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

import com.astuetz.PagerSlidingTabStrip;
import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.servers.connection.HandleViewIoException;
import com.lasthopesoftware.bluewater.servers.connection.InstantiateSessionConnectionActivity;
import com.lasthopesoftware.bluewater.servers.connection.SessionConnection;
import com.lasthopesoftware.bluewater.servers.library.access.LibraryViewsProvider;
import com.lasthopesoftware.bluewater.servers.library.items.IItem;
import com.lasthopesoftware.bluewater.servers.library.items.Item;
import com.lasthopesoftware.bluewater.servers.library.items.access.ItemProvider;
import com.lasthopesoftware.bluewater.servers.library.items.list.IItemListViewContainer;
import com.lasthopesoftware.bluewater.servers.library.items.list.menus.changes.handlers.ItemListMenuChangeHandler;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.nowplaying.NowPlayingFloatingActionButton;
import com.lasthopesoftware.bluewater.servers.library.items.menu.LongClickViewAnimatorListener;
import com.lasthopesoftware.bluewater.servers.library.items.playlists.PlaylistListFragment;
import com.lasthopesoftware.bluewater.servers.library.items.playlists.access.PlaylistsProvider;
import com.lasthopesoftware.bluewater.servers.library.repository.Library;
import com.lasthopesoftware.bluewater.servers.library.repository.LibrarySession;
import com.lasthopesoftware.bluewater.servers.library.views.LibraryViewPagerAdapter;
import com.lasthopesoftware.bluewater.servers.library.views.adapters.SelectStaticViewAdapter;
import com.lasthopesoftware.bluewater.servers.library.views.adapters.SelectViewAdapter;
import com.lasthopesoftware.bluewater.shared.SpecialValueHelpers;
import com.lasthopesoftware.bluewater.shared.view.ViewUtils;
import com.lasthopesoftware.threading.ISimpleTask;
import com.lasthopesoftware.threading.ISimpleTask.OnCompleteListener;
import com.lasthopesoftware.threading.Lazy;

import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;

public class BrowseLibraryActivity extends AppCompatActivity implements IItemListViewContainer {

	private static final String SAVED_TAB_KEY = SpecialValueHelpers.buildMagicPropertyName(BrowseLibraryActivity.class, "SAVED_TAB_KEY");
	private static final String SAVED_SCROLL_POS = SpecialValueHelpers.buildMagicPropertyName(BrowseLibraryActivity.class, "SAVED_SCROLL_POS");
    private static final String SAVED_SELECTED_VIEW = SpecialValueHelpers.buildMagicPropertyName(BrowseLibraryActivity.class, "SAVED_SELECTED_VIEW");

	private static final List<String> specialViews = Collections.singletonList("Active Downloads");

	/**
	 * The {@link ViewPager} that will host the section contents.
	 */
	private RelativeLayout browseLibraryContainerRelativeLayout;
	private ViewPager viewPager;
	private ListView selectViewsListView;
	private ListView specialLibraryItemsListView;
	private View activeFileDownloadsView;
	private DrawerLayout drawerLayout;
	private PagerSlidingTabStrip libraryViewsTabs;
	private RelativeLayout tabbedLibraryViewsRelativeLayout;
	private ProgressBar loadingViewsProgressBar;

	private ViewAnimator viewAnimator;
	private NowPlayingFloatingActionButton nowPlayingFloatingActionButton;

	private PlaylistListFragment playlistListFragment;

	private ActionBarDrawerToggle drawerToggle = null;

	private CharSequence oldTitle;
	private boolean isStopped = false;

	private boolean isLibraryChanged = false;

	private final Lazy<OnCompleteListener<Void, Void, List<Item>>> onGetVisibleViewsCompleteListener = new Lazy<>(new Callable<OnCompleteListener<Void, Void, List<Item>>>() {
		@Override
		public OnCompleteListener<Void, Void, List<Item>> call() throws Exception {
			return new OnCompleteListener<Void, Void, List<Item>>() {

				@Override
				public void onComplete(ISimpleTask<Void, Void, List<Item>> owner, List<Item> result) {
					if (isStopped || result == null) return;

					final LibraryViewPagerAdapter viewChildPagerAdapter = new LibraryViewPagerAdapter(getSupportFragmentManager());
					viewChildPagerAdapter.setOnItemListMenuChangeHandler(new ItemListMenuChangeHandler(BrowseLibraryActivity.this));

					viewChildPagerAdapter.setLibraryViews(result);

					// Set up the ViewPager with the sections adapter.
					viewPager.setAdapter(viewChildPagerAdapter);
					libraryViewsTabs.setViewPager(viewPager);

					libraryViewsTabs.setVisibility(result.size() <= 1 ? View.GONE : View.VISIBLE);

					showContainerView(tabbedLibraryViewsRelativeLayout);
				}
			};
		}
	});
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
            if (intent.hasCategory(Intent.CATEGORY_LAUNCHER) && intentAction != null && intentAction.equals(Intent.ACTION_MAIN)) {
                LoggerFactory.getLogger(getClass()).info("Main Activity is not the root.  Finishing Main Activity instead of launching.");
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
		viewPager = (ViewPager) findViewById(R.id.libraryViewPager);
		tabbedLibraryViewsRelativeLayout = (RelativeLayout) findViewById(R.id.tabbedLibraryViewsRelativeLayout);
		loadingViewsProgressBar = (ProgressBar) findViewById(R.id.pbLoadingViews);
		browseLibraryContainerRelativeLayout = (RelativeLayout) findViewById(R.id.browseLibraryContainer);

		libraryViewsTabs = (PagerSlidingTabStrip) findViewById(R.id.tabsLibraryViews);

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

		if (savedInstanceState != null) restoreScrollPosition(savedInstanceState);

		LocalBroadcastManager.getInstance(this).registerReceiver(onLibraryChanged, new IntentFilter(LibrarySession.libraryChosenEvent));

		specialLibraryItemsListView = (ListView) findViewById(R.id.specialLibraryItemsListView);

		final android.app.Fragment activeFileDownloadsFragment = getFragmentManager().findFragmentById(R.id.downloadsFragment);
		if (activeFileDownloadsFragment != null) {
			activeFileDownloadsView = activeFileDownloadsFragment.getView();
			if (activeFileDownloadsView != null)
				activeFileDownloadsView.setVisibility(View.INVISIBLE);
		}

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

		if (!InstantiateSessionConnectionActivity.restoreSessionConnection(this)) getLibrary();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == InstantiateSessionConnectionActivity.ACTIVITY_ID) getLibrary();
	}

	private void getLibrary() {
		isStopped = false;
		if ((selectViewsListView.getAdapter() != null && viewPager.getAdapter() != null)) return;

        showProgressBar();

		LibrarySession.GetActiveLibrary(this, new OnCompleteListener<Integer, Void, Library>() {

			@Override
			public void onComplete(ISimpleTask<Integer, Void, Library> owner, final Library result) {
				// No library, must bail out
				if (result == null) {
					finish();
					return;
				}

				displayLibrary(result);
			}
		});

	}

	public void displayLibrary(final Library library) {
		final Library.ViewType selectedViewType = library.getSelectedViewType();

		specialLibraryItemsListView.setAdapter(new SelectStaticViewAdapter(this, specialViews, selectedViewType, library.getSelectedView()));

		new LibraryViewsProvider(SessionConnection.getSessionConnectionProvider())
				.onComplete(new OnCompleteListener<Void, Void, List<Item>>() {

			        @Override
			        public void onComplete(ISimpleTask<Void, Void, List<Item>> owner, final List<Item> items) {
				        if (isStopped || items == null) return;

				        LongClickViewAnimatorListener.tryFlipToPreviousView(viewAnimator);

				        selectViewsListView.setAdapter(new SelectViewAdapter(BrowseLibraryActivity.this, items, selectedViewType, library.getSelectedView()));
				        selectViewsListView.setOnItemClickListener(getOnSelectViewClickListener(items));

				        if (!Library.serverViewTypes.contains(selectedViewType)) {
					        oldTitle = specialViews.get(0);
					        getSupportActionBar().setTitle(oldTitle);

					        showContainerView(activeFileDownloadsView);
					        return;
				        }

				        for (IItem item : items) {
					        if (item.getKey() != library.getSelectedView()) continue;
					        oldTitle = item.getValue();
					        getSupportActionBar().setTitle(oldTitle);
					        break;
				        }

				        if (selectedViewType == Library.ViewType.PlaylistView) {
					        hideAllViews();

					        final FragmentTransaction ft = getFragmentManager().beginTransaction();
					        try {
						        if (playlistListFragment != null)
							        ft.remove(playlistListFragment);

						        playlistListFragment = new PlaylistListFragment();
						        playlistListFragment.setOnItemListMenuChangeHandler(new ItemListMenuChangeHandler(BrowseLibraryActivity.this));
						        ft.add(R.id.browseLibraryContainer, playlistListFragment);
					        } finally {
						        ft.commit();
					        }

					        return;
				        }

				        ItemProvider
						        .provide(SessionConnection.getSessionConnectionProvider(), library.getSelectedView())
						        .onComplete(onGetVisibleViewsCompleteListener.getObject())
						        .onError(new HandleViewIoException<Void, Void, List<Item>>(BrowseLibraryActivity.this, new Runnable() {

							        @Override
							        public void run() {
								        ItemProvider
										        .provide(SessionConnection.getSessionConnectionProvider(), library.getSelectedView())
										        .onComplete(onGetVisibleViewsCompleteListener.getObject())
										        .onError(new HandleViewIoException<Void, Void, List<Item>>(BrowseLibraryActivity.this, this))
										        .execute();
							        }

						        }))
						        .execute();
			        }
                })
				.onError(new HandleViewIoException<Void, Void, List<Item>>(this, new Runnable() {

					@Override
					public void run() {
						// Get a new instance of the file system as the connection provider may have changed
						displayLibrary(library);
					}
				}))
				.execute(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	private OnItemClickListener getOnSelectViewClickListener(final List<Item> items) {
		return new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				final Item selectedItem = items.get(position);
				updateSelectedView(PlaylistsProvider.PlaylistsItemKey.equals(selectedItem.getValue()) ? Library.ViewType.PlaylistView : Library.ViewType.StandardServerView, selectedItem.getKey());
			}
		};
	}

	private void updateSelectedView(final Library.ViewType selectedViewType, final int selectedViewKey) {
		drawerLayout.closeDrawer(GravityCompat.START);
		drawerToggle.syncState();

		LibrarySession.GetActiveLibrary(this, new OnCompleteListener<Integer, Void, Library>() {

			@Override
			public void onComplete(ISimpleTask<Integer, Void, Library> owner, final Library library) {
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

	@Override
	public void onSaveInstanceState(final Bundle savedInstanceState) {
		super.onSaveInstanceState(savedInstanceState);

		if (viewPager == null) return;

        savedInstanceState.putInt(SAVED_TAB_KEY, viewPager.getCurrentItem());
		savedInstanceState.putInt(SAVED_SCROLL_POS, viewPager.getScrollY());
		LibrarySession.GetActiveLibrary(this, new OnCompleteListener<Integer, Void, Library>() {
			@Override
			public void onComplete(ISimpleTask<Integer, Void, Library> owner, Library library) {
				if (library != null)
					savedInstanceState.putInt(SAVED_SELECTED_VIEW, library.getSelectedView());
			}
		});
	}

	@Override
	public void onRestoreInstanceState(@NonNull final Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);

		restoreScrollPosition(savedInstanceState);
	}

    private void restoreScrollPosition(final Bundle savedInstanceState) {
        if (viewPager == null) return;

        LibrarySession.GetActiveLibrary(this, new OnCompleteListener<Integer, Void, Library>() {

	        @Override
	        public void onComplete(ISimpleTask<Integer, Void, Library> owner, Library library) {
		        final int savedSelectedView = savedInstanceState.getInt(SAVED_SELECTED_VIEW, -1);
		        if (savedSelectedView < 0 || savedSelectedView != library.getSelectedView()) return;

		        final int savedTabKey = savedInstanceState.getInt(SAVED_TAB_KEY, -1);
		        if (savedTabKey > -1)
			        viewPager.setCurrentItem(savedTabKey);

		        final int savedScrollPosition = savedInstanceState.getInt(SAVED_SCROLL_POS, -1);
		        if (savedScrollPosition > -1)
			        viewPager.setScrollY(savedScrollPosition);
	        }
        });
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
