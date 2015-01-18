package com.lasthopesoftware.bluewater.servers.library.items.files;

import com.lasthopesoftware.bluewater.data.service.objects.File;
import com.lasthopesoftware.bluewater.servers.library.items.files.details.FileDetailsActivity;

import android.content.Intent;
import android.view.View;
import android.view.View.OnClickListener;

public class ViewFileDetailsClickListener implements OnClickListener {

	private File mFile;
	
	public ViewFileDetailsClickListener(File file) {
		mFile = file;
	}
	
	@Override
	public void onClick(View v) {
		Intent intent = new Intent(v.getContext(), FileDetailsActivity.class);
		intent.putExtra(FileDetailsActivity.FILE_KEY, mFile.getKey());
		v.getContext().startActivity(intent);
	}
}
