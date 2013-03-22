package com.lasthopesoftware.jrmediastreamer;

import java.util.ArrayList;

import jrAccess.JrSession;
import jrFileSystem.JrPlaylist;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;

public class ClickPlaylistListener implements OnItemClickListener {

	private ArrayList<JrPlaylist> mPlaylists;
	private Context mContext;
	
	public ClickPlaylistListener(Context context, ArrayList<JrPlaylist> playlists) {
		mContext = context;
		mPlaylists = playlists;
	}
	
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		Intent playlistIntent = new Intent(mContext, ViewPlaylists.class);
		JrSession.selectedItem = mPlaylists.get(position);
		playlistIntent.putExtra(ViewPlaylists.KEY, mPlaylists.get(position).getKey());
		mContext.startActivity(playlistIntent);
	}

}
