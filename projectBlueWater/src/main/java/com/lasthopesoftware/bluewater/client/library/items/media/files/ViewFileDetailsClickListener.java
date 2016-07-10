package com.lasthopesoftware.bluewater.client.library.items.media.files;

import android.content.Intent;
import android.view.View;

import com.lasthopesoftware.bluewater.client.library.items.media.files.details.FileDetailsActivity;
import com.lasthopesoftware.bluewater.client.library.items.menu.NotifyOnFlipViewAnimator;
import com.lasthopesoftware.bluewater.client.library.items.menu.handlers.AbstractMenuClickHandler;

public class ViewFileDetailsClickListener extends AbstractMenuClickHandler {

	private final IFile mFile;
	
	public ViewFileDetailsClickListener(NotifyOnFlipViewAnimator viewFlipper, IFile file) {
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
