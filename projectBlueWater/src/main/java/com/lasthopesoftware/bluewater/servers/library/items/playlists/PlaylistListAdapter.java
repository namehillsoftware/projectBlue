package com.lasthopesoftware.bluewater.servers.library.items.playlists;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import com.lasthopesoftware.bluewater.servers.library.items.menu.ItemMenu;

import java.util.List;

public class PlaylistListAdapter extends ArrayAdapter<Playlist> {
		
	
	public PlaylistListAdapter(Context context, int resource, List<Playlist> objects) {
		super(context, resource, objects);
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		return ItemMenu.getView(getItem(position), convertView, parent);
	}
}
