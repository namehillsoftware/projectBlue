package com.lasthopesoftware.bluewater;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import jrAccess.JrDataTask;
import jrAccess.JrDataTask.OnConnectListener;
import jrAccess.JrFsResponseHandler;
import jrAccess.JrSession;
import jrFileSystem.IJrItem;
import jrFileSystem.JrFileSystem;
import jrFileSystem.JrItem;
import jrFileSystem.JrPlaylists;
import android.app.ActionBar;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

public class BrowseLibrary extends FragmentActivity implements ActionBar.TabListener {

    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide fragments for each of the
     * sections. We use a {@link android.support.v4.app.FragmentPagerAdapter} derivative, which will
     * keep every loaded fragment in memory. If this becomes too memory intensive, it may be best
     * to switch to a {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
    SectionsPagerAdapter mSectionsPagerAdapter;
    
    
    
    /**
     * The {@link ViewPager} that will host the section contents.
     */
    ViewPager mViewPager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("Library");
        if (!JrSession.CreateSession(getSharedPreferences(JrSession.PREFS_FILE, 0))) {
        	displayConnectionSetup();
        	return;
        }
        
        displayLibrary();
    }
    
    
    
    private void displayConnectionSetup() {
    	Intent intent = new Intent(this, ConnectionSettings.class);
    	startActivity(intent);
    }
    
    private void displayLibrary() {
    	setContentView(R.layout.activity_stream_media);
        
    	if (JrSession.jrFs == null) JrSession.jrFs = new JrFileSystem();
    	
        mSectionsPagerAdapter = new SectionsPagerAdapter(this, getSupportFragmentManager());
        // Set up the action bar.
        final ActionBar actionBar = getActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        

        // Set up the ViewPager with the sections adapter.       
        mViewPager = (ViewPager) findViewById(R.id.pager);        
        mViewPager.setAdapter(mSectionsPagerAdapter);

        // When swiping between different sections, select the corresponding tab.
        // We can also use ActionBar.Tab#select() to do this if we have a reference to the
        // Tab.
        mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                actionBar.setSelectedNavigationItem(position);
            }
        });

        // For each of the sections in the app, add a tab to the action bar.
        for (int i = 0; i < mSectionsPagerAdapter.getCount(); i++) {
            // Create a tab with text corresponding to the page title defined by the adapter.
            // Also specify this Activity object, which implements the TabListener interface, as the
            // listener for when this tab is selected.
            actionBar.addTab(actionBar.newTab()
                    .setText(mSectionsPagerAdapter.getPageTitle(i))
                    .setTabListener(this));
        }
    }

    @Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.menu_blue_water, menu);
		menu.findItem(R.id.menu_view_now_playing).setVisible(ViewUtils.displayNowPlayingMenu());
		return true;
	}
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	return ViewUtils.handleMenuClicks(this, item);
    }
    
    @Override
    public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
    }

    @Override
    public void onTabSelected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
        // When the given tab is selected, switch to the corresponding page in the ViewPager.
        mViewPager.setCurrentItem(tab.getPosition());
    }

    @Override
    public void onTabReselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to one of the primary
     * sections of the app.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {
    	private Integer mCount;
    	private Context mContext;
    	
        public SectionsPagerAdapter(Context context, FragmentManager fm) {
            super(fm);
            mContext = context;
        }

        @Override
        public Fragment getItem(int i) {
            Fragment fragment = new CategoryFragment();
            Bundle args = new Bundle();
            args.putInt(CategoryFragment.ARG_CATEGORY_POSITION, i);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public int getCount() {
            if (mCount == null) {
                mCount = getPages().size();
            }
            return mCount;
        }

        @Override
        public CharSequence getPageTitle(int position) {
        	
            return getPages().get(position).getValue() != null ? getPages().get(position).getValue().toUpperCase(Locale.ENGLISH) : "";
            
        }
        
        public ArrayList<IJrItem> getPages() {
        	if (JrSession.categories == null) {
        		JrSession.categories = new ArrayList<IJrItem>();
        		JrDataTask<List<JrItem>> getSubItemsTask = new JrDataTask<List<JrItem>>(mContext);
        		getSubItemsTask.addOnConnectListener(new OnConnectListener<List<JrItem>>() {

					@Override
					public List<JrItem> onConnect(InputStream is) {
						SAXParserFactory parserFactory = SAXParserFactory.newInstance();
						SAXParser sp = parserFactory.newSAXParser();
				    	JrFsResponseHandler<JrItem> jrResponseHandler;// = new JrFsResponseHandler<JrItem>(newClass);
				    	sp.parse(is, jrResponseHandler);
				    	
				    	return jrResponseHandler.items;
					}
				});
        		
        		List<JrItem> subItems = getSubItemsTask.execute(JrSession.jrFs.getSubItemParams()).get();
        		for (JrItem page : subItems) {
        			if (page.getKey() == 1) {
        				JrSession.categories = ((IJrItem)page).getSubItems();
        				break;
        			}
        		}
        		// remove any categories that do not have any items
        		int i = 0;
        		while (i < JrSession.categories.size()) {
        			if (JrSession.categories.get(i).getSubItems().size() < 1) {
        				JrSession.categories.remove(i);
        				continue;
        			}
        			i++;
        		}
        		
        		JrSession.categories.add(new JrPlaylists(JrSession.categories.size()));
        	}
        	
        	return JrSession.categories;
        }
    }

    public static class SelectedItem extends Fragment {
    	private ListView mListView;
    	public static final String ARG_SELECTED_POSITION = "selected_position";   
    	public static final String ARG_CATEGORY_POSITION = "category_position";
    	
    	public SelectedItem() {
    		super();
    	}
    	

    	@Override
    	public View onCreateView(LayoutInflater inflater, ViewGroup container,
    		Bundle savedInstanceState) {
    		
    		mListView = new ListView(getActivity());
    		return mListView;
    	}
    }    
    
    
}
