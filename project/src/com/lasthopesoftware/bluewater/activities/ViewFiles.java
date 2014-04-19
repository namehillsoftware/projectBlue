package com.lasthopesoftware.bluewater.activities;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.ProgressBar;

import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.activities.adapters.FileListAdapter;
import com.lasthopesoftware.bluewater.activities.common.ViewUtils;
import com.lasthopesoftware.bluewater.activities.listeners.ClickFileListener;
import com.lasthopesoftware.bluewater.data.service.access.IJrDataTask;
import com.lasthopesoftware.bluewater.data.service.access.connection.PollConnectionTask;
import com.lasthopesoftware.bluewater.data.service.objects.IJrFilesContainer;
import com.lasthopesoftware.bluewater.data.service.objects.IJrItem;
import com.lasthopesoftware.bluewater.data.service.objects.JrFile;
import com.lasthopesoftware.bluewater.data.service.objects.JrFiles;
import com.lasthopesoftware.bluewater.data.service.objects.JrItem;
import com.lasthopesoftware.bluewater.data.service.objects.JrPlaylist;
import com.lasthopesoftware.threading.ISimpleTask;
import com.lasthopesoftware.threading.ISimpleTask.OnCompleteListener;
import com.lasthopesoftware.threading.SimpleTaskState;

public class ViewFiles extends FragmentActivity {

	public static final String KEY = "com.lasthopesoftware.bluewater.activities.ViewFiles.key";
	public static final String VALUE = "value";
	public static final String VIEW_ITEM_FILES = "view_item_files";
	public static final String VIEW_PLAYLIST_FILES = "view_playlist_files";
	
	private int mItemId;
	private IJrItem<?> mItem;
	
	private ProgressBar pbLoading;
	private ListView fileListView;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        getActionBar().setDisplayHomeAsUpEnabled(true);
        setContentView(R.layout.activity_view_files);
        fileListView = (ListView)findViewById(R.id.lvFilelist);
        pbLoading = (ProgressBar)findViewById(R.id.pbLoadingFileList);
        
        fileListView.setVisibility(View.INVISIBLE);
        pbLoading.setVisibility(View.VISIBLE);
        if (savedInstanceState != null) mItemId = savedInstanceState.getInt(KEY);
        if (mItemId == 0) mItemId = this.getIntent().getIntExtra(KEY, 1);
        mItem = this.getIntent().getAction().equals(VIEW_PLAYLIST_FILES) ? new JrPlaylist(mItemId) : new JrItem(mItemId);
        
        this.setTitle(this.getIntent().getStringExtra(VALUE));
        final JrFiles filesContainer = (JrFiles)((IJrFilesContainer)mItem).getJrFiles();
        final ViewFiles _this = this;
        filesContainer.setOnFilesCompleteListener(new IJrDataTask.OnCompleteListener<List<JrFile>>() {
			
			@Override
			public void onComplete(ISimpleTask<String, Void, List<JrFile>> owner, List<JrFile> result) {
				if (owner.getState() == SimpleTaskState.ERROR) {
					for (Exception exception : owner.getExceptions()) {
						if (!(exception instanceof IOException)) continue;
						
						PollConnectionTask.Instance.get().addOnCompleteListener(new OnCompleteListener<String, Void, Boolean>() {
							
							@Override
							public void onComplete(ISimpleTask<String, Void, Boolean> owner, Boolean result) {
								filesContainer.getFilesAsync();
							}
						});
						PollConnectionTask.Instance.get().startPolling();
						
						_this.startActivity(new Intent(_this, WaitForConnection.class));
						
						break;
					}
					return;
				}
				
				if (result == null) return;
				
				FileListAdapter fileListAdapter = new FileListAdapter((ArrayList<JrFile>) result);
		    			    	
		    	fileListView.setOnItemClickListener(new ClickFileListener(((IJrFilesContainer)mItem).getJrFiles()));
		    	fileListView.setAdapter(fileListAdapter);
		    	
		    	fileListView.setVisibility(View.VISIBLE);
		        pbLoading.setVisibility(View.INVISIBLE);
			}
		});
        
        filesContainer.getFilesAsync();
	}
	
	@Override
	public void onSaveInstanceState(Bundle savedInstanceState) {
		super.onSaveInstanceState(savedInstanceState);
		savedInstanceState.putInt(KEY, mItemId);
	}
	
	@Override
	public void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		mItemId = savedInstanceState.getInt(KEY);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.menu_blue_water, menu);
		menu.findItem(R.id.menu_view_now_playing).setVisible(ViewUtils.displayNowPlayingMenu(this));
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (ViewUtils.handleNavMenuClicks(this, item)) return true;
		return super.onOptionsItemSelected(item);
	}

}
