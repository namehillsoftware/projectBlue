package com.lasthopesoftware.bluewater;

import java.util.ArrayList;

import com.lasthopesoftware.bluewater.FileSystem.JrPlaylist;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

public class PlaylistAdapter extends BaseAdapter {
	private ArrayList<JrPlaylist> mPlaylists;
	private Context mContext;
	
	public PlaylistAdapter(Context context, ArrayList<JrPlaylist> playlists) {
		mPlaylists = playlists;
		mContext = context;
	}
	
	@Override
	public int getCount() {
		return mPlaylists.size();
	}

	@Override
	public Object getItem(int position) {
		return mPlaylists.get(position);
	}

	@Override
	public long getItemId(int position) {
		return mPlaylists.get(position).getKey();
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		return BrowseItemMenu.getView(mPlaylists.get(position), convertView, parent);
	}
}
