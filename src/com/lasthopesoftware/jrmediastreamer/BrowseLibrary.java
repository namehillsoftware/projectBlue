package com.lasthopesoftware.jrmediastreamer;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import jrAccess.JrAccessDao;
import jrAccess.JrLookUpResponseHandler;
import jrAccess.JrSession;
import jrFileSystem.IJrItem;
import jrFileSystem.JrFileSystem;
import jrFileSystem.JrItem;
import jrFileSystem.JrPlaylists;

import org.apache.http.client.ClientProtocolException;
import android.app.ActionBar;
import android.app.FragmentTransaction;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
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
    
    private OnClickListener mConnectionButtonListener = new OnClickListener() {
        public void onClick(View v) {
        	EditText txtAccessCode = (EditText)findViewById(R.id.txtAccessCode);    	
        	EditText txtUserName = (EditText)findViewById(R.id.txtUserName);
        	EditText txtPassword = (EditText)findViewById(R.id.txtPassword);
        	
        	SharedPreferences.Editor prefsEditor = getPreferences(0).edit();
        	prefsEditor.putString("access_code", txtAccessCode.getText().toString());
        	prefsEditor.putString("user_auth_code", Base64.encodeToString((txtUserName.getText().toString() + ":" + txtPassword.getText().toString()).getBytes(), Base64.DEFAULT).trim());
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
	    	String[] userDetails = decryptedUserAuth.split(":",2);
	    	txtUserName.setText(userDetails[0]);
	    	txtPassword.setText(userDetails[1] != null ? userDetails[1] : "");
    	}

    	Button connectionButton = (Button)findViewById(R.id.btnConnect);
    	connectionButton.setOnClickListener(mConnectionButtonListener);
    }
    
    private void displayLibrary() {
    	setContentView(R.layout.activity_stream_media);
        
    	if (JrSession.jrFs == null) JrSession.jrFs = new JrFileSystem();
    	
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
                mCount = getPages().size();
            }
            return mCount;
        }

        @Override
        public CharSequence getPageTitle(int position) {
        	
            return getPages().get(position).getValue() != null ? getPages().get(position).getValue().toUpperCase() : "";
            
        }
        
        public ArrayList<IJrItem> getPages() {
        	if (JrSession.categories == null) {
        		JrSession.categories = new ArrayList<IJrItem>();
        		for (JrItem page : JrSession.jrFs.getSubItems()) {
        			if (page.getKey() == 1) {        				
        				JrSession.categories = ((IJrItem)page).getSubItems();
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

    public class SelectedItem extends Fragment {
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
