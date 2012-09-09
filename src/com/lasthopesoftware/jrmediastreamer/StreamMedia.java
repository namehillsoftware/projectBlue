package com.lasthopesoftware.jrmediastreamer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import jrAccess.GetJrResponse;
import jrAccess.JrAccessDao;
import jrAccess.JrLookUpResponseHandler;
import jrAccess.JrSession;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.DefaultedHttpParams;
import org.apache.http.params.HttpParams;
import org.xml.sax.InputSource;

import android.app.ActionBar;
import android.app.FragmentTransaction;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.NavUtils;
import android.support.v4.view.ViewPager;
import android.util.SparseArray;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ListView;
import android.widget.TextView;

public class StreamMedia extends FragmentActivity implements ActionBar.TabListener {

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
        setContentView(R.layout.activity_stream_media);
        
        // Create the adapter that will return a fragment for each of the three primary sections
        // of the app.
        
        
        try {
			JrSession.accessDao = new GetMcAccess().execute("oTWRti").get();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());
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
            actionBar.addTab(
                    actionBar.newTab()
                            .setText(mSectionsPagerAdapter.getPageTitle(i))
                            .setTabListener(this));
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_stream_media, menu);
        return true;
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
    	private SparseArray<String> mSections;
    	
        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);           
        }

        @Override
        public Fragment getItem(int i) {
            Fragment fragment = new CategoryFragment(getSections().keyAt(i));
            Bundle args = new Bundle();
            args.putInt(CategoryFragment.ARG_SECTION_NUMBER, i + 1);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public int getCount() {
            if (mCount == null) {
	            mCount = getSections().size();
            }
            return mCount;
        }

        @Override
        public CharSequence getPageTitle(int position) {
        	
            return getSections().valueAt(position) != null ? getSections().valueAt(position).toUpperCase() : "";
            
        }
        
        public SparseArray<String> getSections() {
        	
        	if (mSections == null) {
        		mSections = new SparseArray<String>();
				try {
					Map<String, String> serverSections = (new GetJrResponse()).execute(new String[] { JrSession.accessDao.getValidUrl(), "Browse/Children", "ID=1" }).get().getItems();
					for (Map.Entry<String, String> item : serverSections.entrySet())
						mSections.put(Integer.parseInt(item.getValue()), item.getKey());
				} catch (Exception e) {
					e.printStackTrace();
				}
        	}
        	
        	return mSections;
        }
    }

    /**
     * A dummy fragment representing a section of the app, but that simply displays dummy text.
     */
    public static class CategoryFragment extends Fragment {
    	private int mSectionId;
        public CategoryFragment(int sectionId) {
        	mSectionId = sectionId;
        }

        public static final String ARG_SECTION_NUMBER = "section_number";

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
        	ExpandableListView listView = new ExpandableListView(getActivity());
        	CategoryExpandableListAdapter adapter = new CategoryExpandableListAdapter(getActivity(), mSectionId);
        	listView.setAdapter(adapter);
            return listView;
        }
    }
    
    public static class CategoryExpandableListAdapter extends BaseExpandableListAdapter {
    	Context mContext;
    	SparseArray<String> mGroups;
    	
    	public CategoryExpandableListAdapter(Context context) {
    		mContext = context;
    		try {
				Map<String, String> serverGroups = (new GetJrResponse()).execute(new String[] { JrSession.accessDao.getValidUrl(), "Browse/Children", "ID=" + String.valueOf(sectionId) }).get().getItems();
				
				for (Map.Entry<String, String> item : serverGroups.entrySet())
					mGroups.put(Integer.parseInt(item.getValue()), item.getKey());
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} 
    	}
    	
		@Override
		public Object getChild(int groupPosition, int childPosition) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public long getChildId(int groupPosition, int childPosition) {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public View getChildView(int groupPosition, int childPosition,
				boolean isLastChild, View convertView, ViewGroup parent) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public int getChildrenCount(int groupPosition) {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public Object getGroup(int groupPosition) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public int getGroupCount() {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public long getGroupId(int groupPosition) {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public View getGroupView(int groupPosition, boolean isExpanded,
				View convertView, ViewGroup parent) {

			TextView tv = new TextView(mContext);
			tv.setGravity(Gravity.LEFT);
			tv.setText(mGroups.valueAt(groupPosition));
			
			return tv;
		}

		@Override
		public boolean hasStableIds() {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public boolean isChildSelectable(int groupPosition, int childPosition) {
			// TODO Auto-generated method stub
			return false;
		}
    	
    }
    
    public class GetMcAccess extends AsyncTask<String, Void, JrAccessDao> {

		@Override
		protected JrAccessDao doInBackground(String... params) {
			
			JrAccessDao accessDao = null;
			
	        try {
	        	URLConnection conn = (new URL("http://webplay.jriver.com/libraryserver/lookup?id=" + params[0])).openConnection();
	        	
	        	SAXParserFactory parserFactory = SAXParserFactory.newInstance();
	        	SAXParser sp = parserFactory.newSAXParser();
	        	JrLookUpResponseHandler responseHandler = new JrLookUpResponseHandler();
	        	
//	        	String login = "david:coleyh";
//	        	String encodedLogin = Base64.encode(login.getBytes());
//	        	conn.setRequestProperty("Authorization", "Basic " + encodedLogin);
	        	
	        	InputStream mcResponseStream = conn.getInputStream();

	        	sp.parse(mcResponseStream, responseHandler);
	        	
	        	accessDao = responseHandler.getResponse();
//	        	if (!accessDao.isStatus() || accessDao.getValidUrl().equals(""))
//	        		return null;
	        		
			} catch (ClientProtocolException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (Exception e) {
				e.printStackTrace();
			}
	        
	        return accessDao;
		}
    }
}
