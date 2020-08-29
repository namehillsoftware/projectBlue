package com.lasthopesoftware.bluewater.client.browsing.items.media.files;

import android.content.Context;
import android.view.View;

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.stringlist.FileStringListUtilities;
import com.lasthopesoftware.bluewater.client.browsing.items.menu.NotifyOnFlipViewAnimator;
import com.lasthopesoftware.bluewater.client.browsing.items.menu.handlers.AbstractMenuClickHandler;
import com.lasthopesoftware.bluewater.client.playback.service.PlaybackService;
import com.namehillsoftware.handoff.promises.response.VoidResponse;

import java.util.Collection;

public class FilePlayClickListener extends AbstractMenuClickHandler {
	private final Collection<ServiceFile> serviceFiles;
	private final int position;
	
	public FilePlayClickListener(NotifyOnFlipViewAnimator parent, int position, Collection<ServiceFile> serviceFiles) {
        super(parent);

		this.position = position;
		this.serviceFiles = serviceFiles;
	}
	
	@Override
	public void onClick(View v) {
		final Context context = v.getContext();

		FileStringListUtilities
			.promiseSerializedFileStringList(serviceFiles)
			.then(new VoidResponse<>(fileStringList -> PlaybackService.launchMusicService(context, position, fileStringList)));

        super.onClick(v);
	}
}
