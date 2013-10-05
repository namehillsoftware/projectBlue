package com.lasthopesoftware.bluewater.activities.adapters;

import java.util.ArrayList;

import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import com.lasthopesoftware.bluewater.activities.common.BrowseItemMenu;
import com.lasthopesoftware.bluewater.data.objects.JrPlaylist;

public class PlaylistAdapter extends BaseAdapter {
	private ArrayList<JrPlaylist> mPlaylists;
	
	public PlaylistAdapter(ArrayList<JrPlaylist> playlists) {
		mPlaylists = playlists;
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
