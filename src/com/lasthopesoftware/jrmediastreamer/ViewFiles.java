package com.lasthopesoftware.jrmediastreamer;

import jrFileSystem.IJrItem;
import jrFileSystem.JrItem;
import jrFileSystem.JrPlaylist;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.widget.ListView;

public class ViewFiles extends FragmentActivity {

	public static final String KEY = "key";
	public static final String VIEW_ITEM_FILES = "view_item_files";
	public static final String VIEW_PLAYLIST_FILES = "view_playlist_files";
	
	private IJrItem<?> mItem;
	
	
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
         
        setContentView(R.layout.activity_view_files);
        this.setTitle(mItem.getValue());
        mItem = this.getIntent().getAction().equals(VIEW_PLAYLIST_FILES) ? new JrItem(this.getIntent().getIntExtra(KEY, 1))
        				: new JrPlaylist(this.getIntent().getIntExtra(KEY, 1));
        FileListAdapter fileListAdapter = new FileListAdapter(this, mItem);
    	
    	ListView fileListView = (ListView)findViewById(R.id.lvFilelist);
    	fileListView.setOnItemClickListener(new ClickFile(this, mItem));
    	fileListView.setAdapter(fileListAdapter);
	}
	
	
}
