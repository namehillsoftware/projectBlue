package com.lasthopesoftware.bluewater.activities.adapters;

import java.util.List;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import com.lasthopesoftware.bluewater.activities.common.BrowseItemMenu;
import com.lasthopesoftware.bluewater.data.service.objects.JrPlaylist;

public class PlaylistAdapter extends ArrayAdapter<JrPlaylist> {
		
	
	public PlaylistAdapter(Context context, int resource, List<JrPlaylist> objects) {
		super(context, resource, objects);
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		return BrowseItemMenu.getView(getItem(position), convertView, parent);
	}
}
