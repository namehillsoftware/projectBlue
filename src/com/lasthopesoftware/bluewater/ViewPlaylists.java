package com.lasthopesoftware.bluewater;

import com.lasthopesoftware.bluewater.R;

import jrAccess.JrSession;
import jrFileSystem.JrPlaylist;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.NavUtils;
import android.view.MenuItem;
import android.widget.ListAdapter;
import android.widget.ListView;

public class ViewPlaylists extends FragmentActivity {

	public static final String KEY = "key";   
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_playlists);      
        JrPlaylist playlist = (JrPlaylist) JrSession.selectedItem;
        ListAdapter adapter;
        ListView playlistView = (ListView)findViewById(R.id.lvPlaylist);
        if (playlist.getSubItems().size() > 0) {
        	adapter = new PlaylistAdapter(this, playlist.getSubItems());
        	playlistView.setOnItemClickListener(new ClickPlaylistListener(this, playlist.getSubItems()));
        	playlistView.setOnItemLongClickListener(new BrowseItemMenu.ClickListener());
        } else {
        	adapter = new FileListAdapter(this, playlist);
        	playlistView.setOnItemClickListener(new ClickFileListener(this, playlist));
        }
        	
        
        playlistView.setAdapter(adapter);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (ViewUtils.handleNavMenuClicks(this, item)) return true;
		return super.onOptionsItemSelected(item);
	}
}