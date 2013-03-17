package com.lasthopesoftware.jrmediastreamer;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import jrAccess.JrAccessDao;
import jrAccess.JrLookUpResponseHandler;
import jrAccess.JrSession;
import jrFileSystem.JrCategory;
import jrFileSystem.JrFileSystem;
import jrFileSystem.JrItem;
import jrFileSystem.JrListing;
import jrFileSystem.JrPage;
import org.apache.http.client.ClientProtocolException;
import android.app.ActionBar;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Base64;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseExpandableListAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.ListView;
import android.widget.TextView;

public class BrowseLibrary extends FragmentActivity implements ActionBar.TabListener {

    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide fragments for each of the
     * sections. We use a {@link android.support.v4.app.FragmentPagerAdapter} derivative, which will
     * keep every loaded fragment in memory. If this becomes too memory intensive, it may be best
     * to switch to a {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
    SectionsPagerAdapter mSectionsPagerAdapter;
    JrFileSystem jrFs;
    
    
    /**
     * The {@link ViewPager} that will host the section contents.
     */
    ViewPager mViewPager;
    
    private OnClickListener mConnectionButtonListener = new OnClickListener() {
        public void onClick(View v) {
        	EditText txtAccessCode = (EditText)findViewById(R.id.txtAccessCode);    	
        	EditText txtUserName = (EditText)findViewById(R.id.txtUserName);
        	EditText txtPassword = (EditText)findViewById(R.id.txtPassword);
        	
        	SharedPreferences.Editor prefsEditor = getPreferences(0).edit();
        	prefsEditor.putString("access_code", txtAccessCode.getText().toString());
        	prefsEditor.putString("user_auth_code", Base64.encodeToString((txtUserName.getText().toString() + ":" + txtPassword.getText().toString()).getBytes(), Base64.DEFAULT));
        	prefsEditor.commit();
        	
        	setConnectionValues();
        	
        	if (JrSession.AccessCode == null || JrSession.AccessCode.isEmpty() || !tryConnection()) return;
        	displayLibrary();
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setConnectionValues();
        
        if (JrSession.AccessCode == null || JrSession.AccessCode.isEmpty() || !tryConnection()) {
        	displayConnectionSetup();
        	return;
        }
        
        displayLibrary();
    }
    
    private void setConnectionValues() {
    	SharedPreferences prefs = getPreferences(0);    	
    	JrSession.AccessCode = prefs.getString("access_code", "");
    	JrSession.UserAuthCode = prefs.getString("user_auth_code", "");
    }
    
    private boolean tryConnection() {
    	boolean connectResult = false;
    	try {
			JrSession.accessDao = new GetMcAccess().execute(JrSession.AccessCode).get();
			connectResult = !JrSession.accessDao.getActiveUrl().isEmpty();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	
    	return connectResult;
    }
    
    private void displayConnectionSetup() {
    	setContentView(R.layout.activity_set_up_connection);
    	SharedPreferences prefs = getPreferences(0);

    	EditText txtAccessCode = (EditText)findViewById(R.id.txtAccessCode);    	
    	EditText txtUserName = (EditText)findViewById(R.id.txtUserName);
    	EditText txtPassword = (EditText)findViewById(R.id.txtPassword);
    	
    	txtAccessCode.setText(prefs.getString("access_code", ""));
    	String decryptedUserAuth = new String(Base64.decode(prefs.getString("user_auth_code", ""), Base64.DEFAULT));
    	if (!decryptedUserAuth.isEmpty()) {
	    	String[] userDetails = decryptedUserAuth.split(":",1);
	    	txtUserName.setText(userDetails[0]);
	    	txtPassword.setText(userDetails[1]);
    	}

    	Button connectionButton = (Button)findViewById(R.id.btnConnect);
    	connectionButton.setOnClickListener(mConnectionButtonListener);
    }
    
    private void displayLibrary() {
    	setContentView(R.layout.activity_stream_media);
        
    	jrFs = new JrFileSystem();
    	
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
            actionBar.addTab(actionBar.newTab()
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
    	private List<JrCategory> mCategories;
    	
        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
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
	            mCount = getCategories().size();
            }
            return mCount;
        }

        @Override
        public CharSequence getPageTitle(int position) {
        	
            return getCategories().get(position).mValue != null ? getCategories().get(position).mValue.toUpperCase() : "";
            
        }
        
        public List<JrCategory> getCategories() {
        	if (mCategories == null) {
        		mCategories = new ArrayList<JrCategory>();
        		for (JrPage page : jrFs.getPages()) {
        			if (page.mKey == 1) {
        				JrSession.selectedLibrary = page;
        				mCategories = JrSession.selectedLibrary.getCategories();
        			}
        		}
        		
        		// remove any categories that do not have any items
        		int i = 0;
        		while (i < mCategories.size()) {
        			if (mCategories.get(i).getCategoryItems().size() < 1) {
        				mCategories.remove(i);
        				continue;
        			}
        			i++;
        		}
        	}
        	
        	return mCategories;
        }
    }

	public class SelectedFragment extends Fragment {
		private ListView mListView;
		public static final String ARG_SELECTED_POSITION = "selected_position";   
		public static final String ARG_CATEGORY_POSITION = "category_position";
		
		public SelectedFragment() {
			super();
		}
		
	
		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
			
			mListView = new ListView(getActivity());
//			mListView.setAdapter(new FileListAdapter(getActivity(), mAlbum));
			return mListView;
		}
	}
	   
	
    
    public static class CategoryFragment extends Fragment {
        public CategoryFragment() {
        	super();
        }

        public static final String ARG_CATEGORY_POSITION = "category_position";

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {

        	ExpandableListView listView = new ExpandableListView(getActivity());
        	
        	CategoryExpandableListAdapter adapter = new CategoryExpandableListAdapter(getActivity(), getArguments().getInt(ARG_CATEGORY_POSITION));
        	
        	listView.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
        	    @Override
        	    public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {        	    	
        	    	JrListing selection = (JrListing)parent.getExpandableListAdapter().getChild(groupPosition, childPosition);
        	    	if (selection.getClass() == JrItem.class) {
        	    		Intent intent = new Intent(parent.getContext(), ViewFiles.class);
        	    		JrSession.selectedItem = selection;
        	    		startActivity(intent);
        	    	}
        	        return false;
        	    }
    	    });
        	listView.setAdapter(adapter);
            return listView;
        }
        
        public class CategoryExpandableListAdapter extends BaseExpandableListAdapter {
        	Context mContext;
        	private List<JrItem> mCategoryItems;
        	
        	public CategoryExpandableListAdapter(Context context, int CategoryPosition) {
        		mContext = context;
        		mCategoryItems = JrSession.selectedLibrary.getCategories().get(CategoryPosition).getCategoryItems();
        	}
        	
    		@Override
    		public Object getChild(int groupPosition, int childPosition) {
    			return mCategoryItems.get(groupPosition).getSubItems().get(childPosition);
    		}

    		@Override
    		public long getChildId(int groupPosition, int childPosition) {
    			return mCategoryItems.get(groupPosition).getSubItems().get(childPosition).mKey;
    		}
    		
    		@Override
    		public View getChildView(int groupPosition, int childPosition,
    			boolean isLastChild, View convertView, ViewGroup parent) {
    			TextView returnView = getGenericView(mContext);
    	//			tv.setGravity(Gravity.LEFT);
    			returnView.setText(mCategoryItems.get(groupPosition).getSubItems().get(childPosition).mValue);
    			return returnView;
    		}

    		@Override
    		public int getChildrenCount(int groupPosition) {
    			// TODO Auto-generated method stub
    			return mCategoryItems.get(groupPosition).getSubItems().size();
    		}

    		@Override
    		public Object getGroup(int groupPosition) {
    			// TODO Auto-generated method stub
    			return mCategoryItems.get(groupPosition);
    		}

    		@Override
    		public int getGroupCount() {
    			// TODO Auto-generated method stub
    			return mCategoryItems.size();
    		}

    		@Override
    		public long getGroupId(int groupPosition) {
    			// TODO Auto-generated method stub
    			return mCategoryItems.get(groupPosition).mKey;
    		}

    		@Override
    		public View getGroupView(int groupPosition, boolean isExpanded,
    				View convertView, ViewGroup parent) {

    			TextView tv = getGenericView(mContext);
//    			tv.setGravity(Gravity.LEFT);
    			tv.setText(mCategoryItems.get(groupPosition).mValue);
    			
    			return tv;
    		}

    		@Override
    		public boolean hasStableIds() {
    			// TODO Auto-generated method stub
    			return true;
    		}

    		@Override
    		public boolean isChildSelectable(int groupPosition, int childPosition) {
    			// TODO Auto-generated method stub
    			return true;
    		}
        	
        }
        
        public TextView getGenericView(Context context) {
            // Layout parameters for the ExpandableListView
            AbsListView.LayoutParams lp = new AbsListView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

            TextView textView = new TextView(context);
            textView.setTextAppearance(context, android.R.style.TextAppearance_Large);
            textView.setLayoutParams(lp);
            // Center the text vertically
            textView.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
//            textView.setTextColor(getResources().getColor(marcyred));
            // Set the text starting position        
            textView.setPadding(64, 20, 20, 20);
            //textView.setHeight(textView.getLineHeight() + 20);
            return textView;
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
	        	
	        	InputStream mcResponseStream = conn.getInputStream();

	        	sp.parse(mcResponseStream, responseHandler);
	        	
	        	accessDao = responseHandler.getResponse();
	        		
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
