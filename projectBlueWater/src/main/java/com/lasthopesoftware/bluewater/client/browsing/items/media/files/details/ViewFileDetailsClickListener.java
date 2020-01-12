package com.lasthopesoftware.bluewater.client.browsing.items.media.files.details;

import android.content.Intent;
import android.view.View;

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.browsing.items.menu.NotifyOnFlipViewAnimator;
import com.lasthopesoftware.bluewater.client.browsing.items.menu.handlers.AbstractMenuClickHandler;

public class ViewFileDetailsClickListener extends AbstractMenuClickHandler {

	private final ServiceFile serviceFile;
	
	public ViewFileDetailsClickListener(NotifyOnFlipViewAnimator viewFlipper, ServiceFile serviceFile) {
        super(viewFlipper);
		this.serviceFile = serviceFile;
	}
	
	@Override
	public void onClick(View v) {
		Intent intent = new Intent(v.getContext(), FileDetailsActivity.class);
		intent.putExtra(FileDetailsActivity.FILE_KEY, serviceFile.getKey());
		v.getContext().startActivity(intent);

        super.onClick(v);
	}
}
