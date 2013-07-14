package com.lasthopesoftware.bluewater;

import jrFileSystem.IJrFilesContainer;
import jrFileSystem.IJrItemFiles;
import jrFileSystem.IJrItem;
import jrFileSystem.JrItem;
import jrFileSystem.JrPlaylist;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;

public class ViewFiles extends FragmentActivity {

	public static final String KEY = "key";
	public static final String VALUE = "value";
	public static final String VIEW_ITEM_FILES = "view_item_files";
	public static final String VIEW_PLAYLIST_FILES = "view_playlist_files";
	
	private IJrItem<?> mItem;
	
	
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        getActionBar().setDisplayHomeAsUpEnabled(true);
        setContentView(R.layout.activity_view_files);
        mItem = this.getIntent().getAction().equals(VIEW_PLAYLIST_FILES) ? new JrPlaylist(this.getIntent().getIntExtra(KEY, 1)) :
        				new JrItem(this.getIntent().getIntExtra(KEY, 1));
        this.setTitle(this.getIntent().getStringExtra(VALUE));
        FileListAdapter fileListAdapter = new FileListAdapter(this, ((IJrFilesContainer)mItem).getJrFiles().getFiles());
    	
    	ListView fileListView = (ListView)findViewById(R.id.lvFilelist);
    	fileListView.setOnItemClickListener(new ClickFileListener(this, ((IJrFilesContainer)mItem).getJrFiles()));
    	fileListView.setAdapter(fileListAdapter);
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
