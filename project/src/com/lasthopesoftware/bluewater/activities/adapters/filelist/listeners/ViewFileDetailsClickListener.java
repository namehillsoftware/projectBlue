package com.lasthopesoftware.bluewater.activities.adapters.filelist.listeners;

import com.lasthopesoftware.bluewater.activities.ViewFileDetails;
import com.lasthopesoftware.bluewater.data.service.objects.File;

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
		Intent intent = new Intent(v.getContext(), ViewFileDetails.class);
		intent.putExtra(ViewFileDetails.FILE_KEY, mFile.getKey());
		v.getContext().startActivity(intent);
	}
}
