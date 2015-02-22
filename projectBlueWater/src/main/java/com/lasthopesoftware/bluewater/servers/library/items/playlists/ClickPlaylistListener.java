package com.lasthopesoftware.bluewater.servers.library.items.playlists;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;

import java.util.ArrayList;

public class ClickPlaylistListener implements OnItemClickListener {

	private ArrayList<Playlist> mPlaylists;
	private Context mContext;
	
	public ClickPlaylistListener(Context context, ArrayList<Playlist> playlists) {
		mContext = context;
		mPlaylists = playlists;
	}
	
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		Intent playlistIntent = new Intent(mContext, PlaylistListActivity.class);
		playlistIntent.putExtra(PlaylistListActivity.KEY, mPlaylists.get(position).getKey());
		mContext.startActivity(playlistIntent);
	}

}
