package com.lasthopesoftware.bluewater.servers.library;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

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
import com.lasthopesoftware.bluewater.servers.library.items.access.ItemProvider;
import com.lasthopesoftware.bluewater.shared.view.ViewUtils;
import com.lasthopesoftware.threading.ISimpleTask;
import com.lasthopesoftware.threading.ISimpleTask.OnCompleteListener;

import java.util.ArrayList;
import java.util.List;

public class BrowseLibraryActivity extends FragmentActivity {

	private static final String SAVED_TAB_KEY = "com.lasthopesoftware.bluewater.activities.BrowseLibrary.SAVED_TAB_KEY";
	private static final String SAVED_SCROLL_POS = "com.lasthopesoftware.bluewater.activities.BrowseLibrary.SAVED_SCROLL_POS";
	
	/**
	 * The {@link ViewPager} that will host the section contents.
	 */
	private ViewPager mViewPager;
	private ListView mLvSelectViews;
	private DrawerLayout mDrawerLayout;

	private ActionBarDrawerToggle mDrawerToggle = null;
	
	private BrowseLibraryActivity mBrowseLibrary = this;
	
	private CharSequence mOldTitle;
	
	private boolean mIsStopped = false;
	
	private OnCompleteListener<String, Void, ArrayList<IItem>> mOnGetVisibleViewsCompleteListener;
		
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_browse_library);
		
		setTitle(R.string.title_activity_library);
		
		getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setHomeButtonEnabled(true);
		
		mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
		mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
		
		mOldTitle = getTitle();
		mDrawerToggle = new ActionBarDrawerToggle(
                this,                  /* host Activity */
                mDrawerLayout,         /* DrawerLayout object */
                R.drawable.ic_drawer,  /* nav drawer icon to replace 'Up' caret */
                R.string.drawer_open,  /* "open drawer" description */
                R.string.drawer_close  /* "close drawer" description */
		) {
			 /** Called when a drawer has settled in a completely closed state. */
			@Override
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
                getActionBar().setTitle(mOldTitle);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }

            /** Called when a drawer has settled in a completely open state. */
			@Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                mOldTitle = getActionBar().getTitle();
                getActionBar().setTitle("Select view");
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }

		};
		
		mDrawerLayout.setDrawerListener(mDrawerToggle);
		mLvSelectViews = (ListView) findViewById(R.id.lvLibraryViewSelection);
		mViewPager = (ViewPager) findViewById(R.id.pager);
	}
	
	@Override
	public void onStart() {
		super.onStart();
		
		if (!InstantiateSessionConnectionActivity.restoreSessionConnection(this)) getLibrary();
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode != InstantiateSessionConnectionActivity.ACTIVITY_ID) return;
				
		getLibrary();
	}
	
	private void getLibrary() {
		mIsStopped = false;
		if ((mLvSelectViews.getAdapter() != null && mViewPager.getAdapter() != null)) return;
		
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
					getActionBar().setTitle(mOldTitle);
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
						mDrawerLayout.closeDrawer(Gravity.START);
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

        libraryViewsProvider.execute();
	}
	
	private OnCompleteListener<String, Void, ArrayList<IItem>> getOnVisibleViewsCompleteListener() {
		if (mOnGetVisibleViewsCompleteListener == null) {
			mOnGetVisibleViewsCompleteListener = new OnCompleteListener<String, Void, ArrayList<IItem>>() {
				
				@Override
				public void onComplete(ISimpleTask<String, Void, ArrayList<IItem>> owner, ArrayList<IItem> result) {
					if (mIsStopped || result == null) return;
					
					final LibraryViewPagerAdapter viewChildPagerAdapter = new LibraryViewPagerAdapter(getSupportFragmentManager());
					viewChildPagerAdapter.setLibraryViews(result);
		
					// Set up the ViewPager with the sections adapter.
					mViewPager.setAdapter(viewChildPagerAdapter);
					((PagerSlidingTabStrip) findViewById(R.id.tabsLibraryViews)).setViewPager(mViewPager);
				}
			};
		}
		
		return mOnGetVisibleViewsCompleteListener;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		return ViewUtils.buildStandardMenu(this, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (mDrawerToggle != null && mDrawerToggle.onOptionsItemSelected(item))
			return true;
		
		return ViewUtils.handleMenuClicks(this, item);
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
	public void onSaveInstanceState(Bundle savedInstanceState) {
		super.onSaveInstanceState(savedInstanceState);
		
		if (mViewPager != null) {
			savedInstanceState.putInt(SAVED_TAB_KEY, mViewPager.getCurrentItem());
			savedInstanceState.putInt(SAVED_SCROLL_POS, mViewPager.getScrollY());
		}
	}
	
	@Override
	public void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		
		if (mViewPager != null) {
			mViewPager.setCurrentItem(savedInstanceState.getInt(SAVED_TAB_KEY));
			mViewPager.setScrollY(savedInstanceState.getInt(SAVED_SCROLL_POS));
		}
	}
	
	@Override
	public void onStop() {
		mIsStopped = true;
		super.onStop();
	}

	public ViewPager getViewPager() {
		return mViewPager;
	}
}
