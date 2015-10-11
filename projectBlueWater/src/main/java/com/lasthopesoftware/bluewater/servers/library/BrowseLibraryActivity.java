package com.lasthopesoftware.bluewater.servers.library;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Bundle;
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
import android.widget.ViewFlipper;

import com.astuetz.PagerSlidingTabStrip;
import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.disk.sqlite.access.LibrarySession;
import com.lasthopesoftware.bluewater.disk.sqlite.objects.Library;
import com.lasthopesoftware.bluewater.servers.connection.HandleViewIoException;
import com.lasthopesoftware.bluewater.servers.connection.InstantiateSessionConnectionActivity;
import com.lasthopesoftware.bluewater.servers.connection.helpers.PollConnection.OnConnectionRegainedListener;
import com.lasthopesoftware.bluewater.servers.library.FileSystem.OnGetFileSystemCompleteListener;
import com.lasthopesoftware.bluewater.servers.library.access.LibraryViewsProvider;
import com.lasthopesoftware.bluewater.servers.library.items.IItem;
import com.lasthopesoftware.bluewater.servers.library.items.Item;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.nowplaying.NowPlayingFloatingActionButton;
import com.lasthopesoftware.bluewater.servers.library.items.menu.LongClickViewFlipListener;
import com.lasthopesoftware.bluewater.servers.library.items.menu.OnViewFlippedListener;
import com.lasthopesoftware.bluewater.shared.view.ViewUtils;
import com.lasthopesoftware.threading.ISimpleTask;
import com.lasthopesoftware.threading.ISimpleTask.OnCompleteListener;

import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class BrowseLibraryActivity extends AppCompatActivity {

	private static final String SAVED_TAB_KEY = "com.lasthopesoftware.bluewater.servers.library.BrowseLibraryActivity.SAVED_TAB_KEY";
	private static final String SAVED_SCROLL_POS = "com.lasthopesoftware.bluewater.servers.library.BrowseLibraryActivity.SAVED_SCROLL_POS";
    private static final String SAVED_SELECTED_VIEW = "com.lasthopesoftware.bluewater.servers.library.BrowseLibraryActivity.SAVED_SELECTED_VIEW";

	/**
	 * The {@link ViewPager} that will host the section contents.
	 */
	private ViewPager mViewPager;
	private ListView mLvSelectViews;
	private DrawerLayout mDrawerLayout;
    private PagerSlidingTabStrip mLibraryViewsTabs;
    private ProgressBar mPbLoadingViews;
    private ViewFlipper mFlippedView;

	private ActionBarDrawerToggle mDrawerToggle = null;

	private BrowseLibraryActivity mBrowseLibrary = this;

	private CharSequence mOldTitle;

	private boolean mIsStopped = false;

	private OnCompleteListener<String, Void, ArrayList<IItem>> mOnGetVisibleViewsCompleteListener;

    private final OnViewFlippedListener mOnViewFlippedListener = new OnViewFlippedListener() {
        @Override
        public void onViewFlipped(ViewFlipper viewFlipper) {
            mFlippedView = viewFlipper;
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

//		ViewUtils.InitializeNowPlayingFloatingActionButton((FloatingActionButton) findViewById(R.id.nowPlayingFloatingActionButton));

		NowPlayingFloatingActionButton.addNowPlayingFloatingActionButton((RelativeLayout) findViewById(R.id.browseLibraryRelativeLayout));

		setTitle(R.string.title_activity_library);

		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		getSupportActionBar().setHomeButtonEnabled(true);

		mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
		mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);

		mOldTitle = getTitle();
		mDrawerToggle = new ActionBarDrawerToggle(
                this,                  /* host Activity */
                mDrawerLayout,         /* DrawerLayout object */
                R.string.drawer_open,  /* "open drawer" description */
                R.string.drawer_close  /* "close drawer" description */
		) {
			 /** Called when a drawer has settled in a completely closed state. */
			@Override
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
				getSupportActionBar().setTitle(mOldTitle);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }

            /** Called when a drawer has settled in a completely open state. */
			@Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                mOldTitle = getSupportActionBar().getTitle();
				getSupportActionBar().setTitle("Select view");
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }

		};

		mDrawerLayout.setDrawerListener(mDrawerToggle);
		mLvSelectViews = (ListView) findViewById(R.id.lvLibraryViewSelection);
		mViewPager = (ViewPager) findViewById(R.id.libraryViewPager);
        mLibraryViewsTabs = (PagerSlidingTabStrip) findViewById(R.id.tabsLibraryViews);
        mPbLoadingViews = (ProgressBar) findViewById(R.id.pbLoadingViews);

        if (savedInstanceState != null) restoreScrollPosition(savedInstanceState);
	}

	@Override
	public void onStart() {
		super.onStart();

		if (!InstantiateSessionConnectionActivity.restoreSessionConnection(this)) getLibrary();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == InstantiateSessionConnectionActivity.ACTIVITY_ID) getLibrary();
	}

	private void getLibrary() {
		mIsStopped = false;
		if ((mLvSelectViews.getAdapter() != null && mViewPager.getAdapter() != null)) return;

        toggleViewsVisibility(false);

		LibrarySession.GetLibrary(mBrowseLibrary, new OnCompleteListener<Integer, Void, Library>() {

			@Override
			public void onComplete(ISimpleTask<Integer, Void, Library> owner, final Library result) {
				FileSystem.Instance.get(mBrowseLibrary, new OnGetFileSystemCompleteListener() {

					@Override
					public void onGetFileSystemComplete(FileSystem fileSystem) {
						displayLibrary(result, fileSystem);
					}
				});
			}
		});

	}

	@SuppressWarnings("unchecked")
	public void displayLibrary(final Library library, final FileSystem fileSystem) {
		final LibraryViewsProvider libraryViewsProvider = new LibraryViewsProvider();

        libraryViewsProvider.onComplete(new OnCompleteListener<Void, Void, List<Item>>() {

			@Override
			public void onComplete(ISimpleTask<Void, Void, List<Item>> owner, final List<Item> items) {
				if (mIsStopped || items == null) return;

				for (IItem item : items) {
					if (item.getKey() != library.getSelectedView()) continue;
					mOldTitle = item.getValue();
					getSupportActionBar().setTitle(mOldTitle);
					break;
				}

				mLvSelectViews.setAdapter(new SelectViewAdapter(mLvSelectViews.getContext(), R.layout.layout_select_views, items, library.getSelectedView()));

				fileSystem.getVisibleViewsAsync(getOnVisibleViewsCompleteListener(),
					new HandleViewIoException(mBrowseLibrary, new OnConnectionRegainedListener() {

						@Override
						public void onConnectionRegained() {
							FileSystem.Instance.get(mBrowseLibrary, new OnGetFileSystemCompleteListener() {

								@Override
								public void onGetFileSystemComplete(FileSystem fileSystem) {
									fileSystem.getVisibleViewsAsync(getOnVisibleViewsCompleteListener());
								}
							});
						}

				}));

				mLvSelectViews.setOnItemClickListener(new OnItemClickListener() {

					@Override
					public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
						mDrawerLayout.closeDrawer(GravityCompat.START);
						mDrawerToggle.syncState();

						final int selectedViewKey = items.get(position).getKey();

						LibrarySession.GetLibrary(mBrowseLibrary, new OnCompleteListener<Integer, Void, Library>() {

							@Override
							public void onComplete(ISimpleTask<Integer, Void, Library> owner, final Library library) {
								if (library.getSelectedView() == selectedViewKey) return;

								library.setSelectedView(selectedViewKey);
								LibrarySession.SaveLibrary(mBrowseLibrary, library);

								FileSystem.Instance.get(mBrowseLibrary, new OnGetFileSystemCompleteListener() {

									@Override
									public void onGetFileSystemComplete(FileSystem fileSystem) {
										displayLibrary(library, fileSystem);
									}
								});
							}
						});
					}
				});
			}
		}).onError(new HandleViewIoException(mBrowseLibrary, new OnConnectionRegainedListener() {

			@Override
			public void onConnectionRegained() {
				FileSystem.Instance.get(mBrowseLibrary, new OnGetFileSystemCompleteListener() {

					@Override
					public void onGetFileSystemComplete(FileSystem fileSystem) {
                        libraryViewsProvider.execute();
					}
				});
			}
		}));

        libraryViewsProvider.execute(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	private OnCompleteListener<String, Void, ArrayList<IItem>> getOnVisibleViewsCompleteListener() {
		if (mOnGetVisibleViewsCompleteListener != null) return mOnGetVisibleViewsCompleteListener;

        mOnGetVisibleViewsCompleteListener = new OnCompleteListener<String, Void, ArrayList<IItem>>() {

            @Override
            public void onComplete(ISimpleTask<String, Void, ArrayList<IItem>> owner, ArrayList<IItem> result) {
                if (mIsStopped || result == null) return;

                final LibraryViewPagerAdapter viewChildPagerAdapter = new LibraryViewPagerAdapter(getSupportFragmentManager());
                viewChildPagerAdapter.setLibraryViews(result);
                viewChildPagerAdapter.setOnViewFlippedListener(mOnViewFlippedListener);

                // Set up the ViewPager with the sections adapter.
                mViewPager.setAdapter(viewChildPagerAdapter);
                mLibraryViewsTabs.setViewPager(mViewPager);

                mLibraryViewsTabs.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
                    @Override
                    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

                    }

                    @Override
                    public void onPageSelected(int position) {
                        LongClickViewFlipListener.tryFlipToPreviousView(mFlippedView);
                    }

                    @Override
                    public void onPageScrollStateChanged(int state) {

                    }
                });

                toggleViewsVisibility(true);
            }
        };

        return mOnGetVisibleViewsCompleteListener;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		return ViewUtils.buildStandardMenu(this, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		return mDrawerToggle != null && mDrawerToggle.onOptionsItemSelected(item) || ViewUtils.handleMenuClicks(this, item);
	}

	@Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        if (mDrawerToggle != null) mDrawerToggle.syncState();
    }

	@Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (mDrawerToggle != null) mDrawerToggle.onConfigurationChanged(newConfig);
    }

	@Override
	public void onSaveInstanceState(final Bundle savedInstanceState) {
		super.onSaveInstanceState(savedInstanceState);

		if (mViewPager == null) return;

        savedInstanceState.putInt(SAVED_TAB_KEY, mViewPager.getCurrentItem());
        savedInstanceState.putInt(SAVED_SCROLL_POS, mViewPager.getScrollY());
        LibrarySession.GetLibrary(this, new OnCompleteListener<Integer, Void, Library>() {
			@Override
			public void onComplete(ISimpleTask<Integer, Void, Library> owner, Library library) {
				if (library != null)
					savedInstanceState.putInt(SAVED_SELECTED_VIEW, library.getSelectedView());
			}
		});
	}

	@Override
	public void onRestoreInstanceState(final Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);

        if (savedInstanceState != null) restoreScrollPosition(savedInstanceState);
	}

    private void restoreScrollPosition(final Bundle savedInstanceState) {
        if (mViewPager == null) return;

        LibrarySession.GetLibrary(this, new OnCompleteListener<Integer, Void, Library>() {

			@Override
			public void onComplete(ISimpleTask<Integer, Void, Library> owner, Library library) {
				final int savedSelectedView = savedInstanceState.getInt(SAVED_SELECTED_VIEW, -1);
				if (savedSelectedView < 0 || savedSelectedView != library.getSelectedView()) return;

				final int savedTabKey = savedInstanceState.getInt(SAVED_TAB_KEY, -1);
				if (savedTabKey > -1)
					mViewPager.setCurrentItem(savedTabKey);

				final int savedScrollPosition = savedInstanceState.getInt(SAVED_SCROLL_POS, -1);
				if (savedScrollPosition > -1)
					mViewPager.setScrollY(savedScrollPosition);
			}
		});
    }

    private void toggleViewsVisibility(boolean isVisible) {
        mLibraryViewsTabs.setVisibility(isVisible ? View.VISIBLE : View.INVISIBLE);
        mViewPager.setVisibility(isVisible ? View.VISIBLE : View.INVISIBLE);
        mPbLoadingViews.setVisibility(isVisible ? View.INVISIBLE : View.VISIBLE);
    }
	
	@Override
	public void onStop() {
		mIsStopped = true;
		super.onStop();
	}

    @Override
    public void onBackPressed() {
        if (LongClickViewFlipListener.tryFlipToPreviousView(mFlippedView)) return;

        super.onBackPressed();
    }
}
