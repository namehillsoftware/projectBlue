package com.lasthopesoftware.bluewater;

import java.util.ArrayList;
import jrFileSystem.IJrItemFiles;
import jrFileSystem.JrFile;
import android.content.Context;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;

public class ClickFileListener implements OnItemClickListener {

	private IJrItemFiles mItem;
	private Context mContext;
	
	public ClickFileListener(Context context, IJrItemFiles item) {
		mContext = context;
		mItem = item;
	}
	
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		ArrayList<JrFile> playlist = mItem.getFiles();
		JrFile file = playlist.get(position);
		StreamingMusicService.StreamMusic(mContext, file, playlist);
	}

}
