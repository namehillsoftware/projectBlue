package com.lasthopesoftware.bluewater;

import java.util.ArrayList;
import java.util.List;

import jrFileSystem.IJrDataTask.OnCompleteListener;
import jrFileSystem.IJrFilesContainer;
import jrFileSystem.IJrItemFiles;
import jrFileSystem.IJrItem;
import jrFileSystem.JrFile;
import jrFileSystem.JrFiles;
import jrFileSystem.JrItem;
import jrFileSystem.JrPlaylist;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.ProgressBar;

public class ViewFiles extends FragmentActivity {

	public static final String KEY = "key";
	public static final String VALUE = "value";
	public static final String VIEW_ITEM_FILES = "view_item_files";
	public static final String VIEW_PLAYLIST_FILES = "view_playlist_files";
	
	private IJrItem<?> mItem;
	private Context mContext;
	
	private ProgressBar pbLoading;
	private ListView fileListView;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        getActionBar().setDisplayHomeAsUpEnabled(true);
        setContentView(R.layout.activity_view_files);
        fileListView = (ListView)findViewById(R.id.lvFilelist);
        pbLoading = (ProgressBar)findViewById(R.id.pbLoadingFileList);
        
        fileListView.setVisibility(View.INVISIBLE);
        pbLoading.setVisibility(View.VISIBLE);
        
        mItem = this.getIntent().getAction().equals(VIEW_PLAYLIST_FILES) ? new JrPlaylist(this.getIntent().getIntExtra(KEY, 1)) :
        				new JrItem(this.getIntent().getIntExtra(KEY, 1));
        this.setTitle(this.getIntent().getStringExtra(VALUE));
        JrFiles filesContainer = (JrFiles)((IJrFilesContainer)mItem).getJrFiles();
        
        filesContainer.setOnFilesCompleteListener(new OnCompleteListener<List<JrFile>>() {
			
			@Override
			public void onComplete(List<JrFile> result) {
				ArrayList<JrFile> innerResult =  (ArrayList<JrFile>) result;
				FileListAdapter fileListAdapter = new FileListAdapter(mContext, innerResult);
		    			    	
		    	fileListView.setOnItemClickListener(new ClickFileListener(mContext, ((IJrFilesContainer)mItem).getJrFiles()));
		    	fileListView.setAdapter(fileListAdapter);
		    	
		    	fileListView.setVisibility(View.VISIBLE);
		        pbLoading.setVisibility(View.INVISIBLE);
			}
		});
        
        filesContainer.getFilesAsync();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.menu_blue_water, menu);
		menu.findItem(R.id.menu_view_now_playing).setVisible(ViewUtils.displayNowPlayingMenu());
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (ViewUtils.handleNavMenuClicks(this, item)) return true;
		return super.onOptionsItemSelected(item);
	}

}
