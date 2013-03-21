package com.lasthopesoftware.jrmediastreamer;

import java.util.ArrayList;

import jrFileSystem.JrPlaylist;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;

public class ClickPlaylist implements OnItemClickListener {

	private ArrayList<JrPlaylist> mPlaylists;
	private Context mContext;
	
	public ClickPlaylist(Context context, ArrayList<JrPlaylist> playlists) {
		mContext = context;
		mPlaylists = playlists;
	}
	
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		Intent playlistIntent = new Intent(mContext, ViewPlaylists.class);
		playlistIntent.putExtra(ViewPlaylists.KEY, mPlaylists.get(position).getKey());
		mContext.startActivity(playlistIntent);
	}

}
