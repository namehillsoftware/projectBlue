package com.lasthopesoftware.bluewater.activities.listeners;

import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;

import com.lasthopesoftware.bluewater.data.objects.IJrItemFiles;
import com.lasthopesoftware.bluewater.services.StreamingMusicService;

public class ClickFileListener implements OnItemClickListener {

	private IJrItemFiles mItem;
	
	public ClickFileListener(IJrItemFiles item) {
		mItem = item;
	}
	
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		StreamingMusicService.StreamMusic(view.getContext(), mItem.getFiles().get(position).getKey(), mItem.getFileStringList());
	}

}
