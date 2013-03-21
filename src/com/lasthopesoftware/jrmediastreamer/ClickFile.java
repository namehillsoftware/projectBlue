package com.lasthopesoftware.jrmediastreamer;

import jrAccess.JrSession;
import jrFileSystem.IJrItem;
import jrFileSystem.JrFile;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;

public class ClickFile implements OnItemClickListener {

	private IJrItem<?> mItem;
	private Context mContext;
	
	public ClickFile(Context context, IJrItem<?> item) {
		mContext = context;
		mItem = item;
	}
	
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		JrSession.playlist = mItem.getFiles();
		JrFile file = mItem.getFiles().get(position);
		Intent svcIntent = new Intent(StreamingMusicService.ACTION_PLAY, Uri.parse(file.getUrl()), mContext, StreamingMusicService.class);
		mContext.startService(svcIntent);

	}

}
