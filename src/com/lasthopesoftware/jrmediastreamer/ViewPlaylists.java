package com.lasthopesoftware.jrmediastreamer;

import jrFileSystem.JrPlaylist;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.widget.ListAdapter;
import android.widget.ListView;

public class ViewPlaylists extends FragmentActivity {

	public static final String KEY = "key";   
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_files);       
        int playlistKey = this.getIntent().getIntExtra(ViewPlaylists.KEY, -1);
        JrPlaylist playlist = new JrPlaylist(playlistKey);
        ListAdapter adapter;
        ListView playlistView = (ListView)findViewById(R.id.lvPlaylist);
        if (playlist.getSubItems().size() > 0) {
        	adapter = new PlaylistAdapter(this, playlist.getSubItems());
        	playlistView.setOnItemClickListener(new ClickPlaylist(this, playlist.getSubItems()));
        } else {
        	adapter = new FileListAdapter(this, playlist);
        }
        	
        
        playlistView.setAdapter(adapter);
	}
	
	
}