package com.lasthopesoftware.bluewater.servers.library;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
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
import com.lasthopesoftware.bluewater.servers.library.items.list.IItemListViewContainer;
import com.lasthopesoftware.bluewater.servers.library.items.list.menus.changes.handlers.ItemListMenuChangeHandler;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.nowplaying.NowPlayingFloatingActionButton;
import com.lasthopesoftware.bluewater.servers.library.items.menu.LongClickViewAnimatorListener;
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

import java.util.ArrayList;
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
	private ViewPager viewPager;
	private ListView selectViewsListView;
	private View activeFileDownloadsView;
	private DrawerLayout drawerLayout;
	private PagerSlidingTabStrip libraryViewsTabs;
	private RelativeLayout tabbedLibraryViewsRelativeLayout;
	private ProgressBar loadingViewsProgressBar;

	private ViewAnimator viewAnimator;

	private NowPlayingFloatingActionButton nowPlayingFloatingActionButton;

	private ActionBarDrawerToggle drawerToggle = null;

	private BrowseLibraryActivity browseLibraryActivity = this;
	private CharSequence oldTitle;

	private boolean isStopped = false;

	private boolean isLibraryChanged = false;

	private final Lazy<OnCompleteListener<String, Void, ArrayList<IItem>>> onGetVisibleViewsCompleteListener = new Lazy<>(new Callable<OnCompleteListener<String, Void, ArrayList<IItem>>>() {
		@Override
		public OnCompleteListener<String, Void, ArrayList<IItem>> call() throws Exception {
			return new OnCompleteListener<String, Void, ArrayList<IItem>>() {

				@Override
				public void onComplete(ISimpleTask<String, Void, ArrayList<IItem>> owner, ArrayList<IItem> result) {
					if (isStopped || result == null) return;

					final LibraryViewPagerAdapter viewChildPagerAdapter = new LibraryViewPagerAdapter(getSupportFragmentManager());
					viewChildPagerAdapter.setOnItemListMenuChangeHandler(new ItemListMenuChangeHandler(BrowseLibraryActivity.this));

					viewChildPagerAdapter.setLibraryViews(result);

					// Set up the ViewPager with the sections adapter.
					viewPager.setAdapter(viewChildPagerAdapter);
					libraryViewsTabs.setViewPager(viewPager);

					libraryViewsTabs.setVisibility(result.size() <= 1 ? View.GONE : View.VISIBLE);

					toggleViewsVisibility(true);
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


		final ListView specialLibraryItemsListView = (ListView) findViewById(R.id.specialLibraryItemsListView);
		final SelectStaticViewAdapter selectStaticViewAdapter = new SelectStaticViewAdapter(this, specialViews);
		specialLibraryItemsListView.setAdapter(selectStaticViewAdapter);

		final Fragment activeFileDownloadsFragment = getSupportFragmentManager().findFragmentById(R.id.downloadsFragment);
		if (activeFileDownloadsFragment == null) return;

		activeFileDownloadsView = activeFileDownloadsFragment.getView();
		if (activeFileDownloadsView == null) return;

		specialLibraryItemsListView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				selectStaticViewAdapter.setSelectedItem(specialViews.get(position));
				loadingViewsProgressBar.setVisibility(ViewUtils.getVisibility(false));
				tabbedLibraryViewsRelativeLayout.setVisibility(ViewUtils.getVisibility(false));
				activeFileDownloadsView.setVisibility(ViewUtils.getVisibility(true));
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

        toggleViewsVisibility(false);

		LibrarySession.GetActiveLibrary(browseLibraryActivity, new OnCompleteListener<Integer, Void, Library>() {

			@Override
			public void onComplete(ISimpleTask<Integer, Void, Library> owner, final Library result) {
				// No library, must bail out
				if (result == null) {
					finish();
					return;
				}

				displayLibrary(result, new FileSystem(SessionConnection.getSessionConnectionProvider(), result));
			}
		});

	}

	public void displayLibrary(final Library library, final FileSystem fileSystem) {
		final LibraryViewsProvider libraryViewsProvider = new LibraryViewsProvider(SessionConnection.getSessionConnectionProvider());

        libraryViewsProvider.onComplete(new OnCompleteListener<Void, Void, List<Item>>() {

			@Override
			public void onComplete(ISimpleTask<Void, Void, List<Item>> owner, final List<Item> items) {
				if (isStopped || items == null) return;

				LongClickViewAnimatorListener.tryFlipToPreviousView(viewAnimator);

				for (IItem item : items) {
					if (item.getKey() != library.getSelectedView()) continue;
					oldTitle = item.getValue();
					getSupportActionBar().setTitle(oldTitle);
					break;
				}

				selectViewsListView.setAdapter(new SelectViewAdapter(selectViewsListView.getContext(), items, library.getSelectedView()));
				fileSystem.getVisibleViewsAsync(onGetVisibleViewsCompleteListener.getObject(),
						new HandleViewIoException<String, Void, ArrayList<IItem>>(browseLibraryActivity, new Runnable() {

							@Override
							public void run() {
								new FileSystem(SessionConnection.getSessionConnectionProvider(), library)
										.getVisibleViewsAsync(onGetVisibleViewsCompleteListener.getObject(), new HandleViewIoException<String, Void, ArrayList<IItem>>(browseLibraryActivity, this));
							}

						})
				);

				selectViewsListView.setOnItemClickListener(getOnSelectViewClickListener(items));
			}
		}).onError(new HandleViewIoException<Void, Void, List<Item>>(browseLibraryActivity, new Runnable() {

	        @Override
	        public void run() {
		        // Get a new instance of the file system as the connection provider may have changed
		        displayLibrary(library, new FileSystem(SessionConnection.getSessionConnectionProvider(), library));
			}
		}));

        libraryViewsProvider.execute(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	private OnItemClickListener getOnSelectViewClickListener(final List<Item> items) {

		return new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				drawerLayout.closeDrawer(GravityCompat.START);
				drawerToggle.syncState();

				final int selectedViewKey = items.get(position).getKey();

				LibrarySession.GetActiveLibrary(browseLibraryActivity, new OnCompleteListener<Integer, Void, Library>() {

					@Override
					public void onComplete(ISimpleTask<Integer, Void, Library> owner, final Library library) {
						if (library.getSelectedView() == selectedViewKey) return;

						library.setSelectedView(selectedViewKey);
						library.setSelectedViewType(Library.ViewType.ServerView);
						LibrarySession.SaveLibrary(browseLibraryActivity, library);

						displayLibrary(library, new FileSystem(SessionConnection.getSessionConnectionProvider(), library));
					}
				});
			}
		};
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

    private void toggleViewsVisibility(boolean isVisible) {
        tabbedLibraryViewsRelativeLayout.setVisibility(ViewUtils.getVisibility(isVisible));
	    loadingViewsProgressBar.setVisibility(ViewUtils.getVisibility(!isVisible));

	    if (activeFileDownloadsView != null)
		    activeFileDownloadsView.setVisibility(ViewUtils.getVisibility(false));
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
