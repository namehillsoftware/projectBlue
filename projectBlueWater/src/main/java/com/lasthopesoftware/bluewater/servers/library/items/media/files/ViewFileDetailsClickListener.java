package com.lasthopesoftware.bluewater.servers.library.items.media.files;

import android.content.Intent;
import android.view.View;
import android.widget.ViewFlipper;

import com.lasthopesoftware.bluewater.servers.library.items.media.files.details.FileDetailsActivity;
import com.lasthopesoftware.bluewater.servers.library.items.menu.handlers.AbstractMenuClickHandler;

public class ViewFileDetailsClickListener extends AbstractMenuClickHandler {

	private final IFile mFile;
	
	public ViewFileDetailsClickListener(ViewFlipper viewFlipper, IFile file) {
        super(viewFlipper);
		mFile = file;
	}
	
	@Override
	public void onClick(View v) {
		Intent intent = new Intent(v.getContext(), FileDetailsActivity.class);
		intent.putExtra(FileDetailsActivity.FILE_KEY, mFile.getKey());
		v.getContext().startActivity(intent);

        super.onClick(v);
	}
}
