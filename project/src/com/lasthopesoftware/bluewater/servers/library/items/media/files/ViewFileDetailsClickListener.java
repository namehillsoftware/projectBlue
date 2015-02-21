package com.lasthopesoftware.bluewater.servers.library.items.media.files;

import android.content.Intent;
import android.view.View;
import android.view.View.OnClickListener;

import com.lasthopesoftware.bluewater.servers.library.items.media.files.details.FileDetailsActivity;

public class ViewFileDetailsClickListener implements OnClickListener {

	private IFile mFile;
	
	public ViewFileDetailsClickListener(IFile file) {
		mFile = file;
	}
	
	@Override
	public void onClick(View v) {
		Intent intent = new Intent(v.getContext(), FileDetailsActivity.class);
		intent.putExtra(FileDetailsActivity.FILE_KEY, mFile.getKey());
		v.getContext().startActivity(intent);
	}
}
