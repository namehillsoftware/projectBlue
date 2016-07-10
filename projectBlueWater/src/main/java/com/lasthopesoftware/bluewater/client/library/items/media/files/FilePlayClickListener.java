package com.lasthopesoftware.bluewater.client.library.items.media.files;

import android.view.View;

import com.lasthopesoftware.bluewater.client.library.items.media.files.access.stringlist.FileStringListUtilities;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.service.PlaybackService;
import com.lasthopesoftware.bluewater.client.library.items.menu.NotifyOnFlipViewAnimator;
import com.lasthopesoftware.bluewater.client.library.items.menu.handlers.AbstractMenuClickHandler;

import java.util.List;

public class FilePlayClickListener extends AbstractMenuClickHandler {
	private final List<IFile> mFiles;
	private final int mPosition;
	
	public FilePlayClickListener(NotifyOnFlipViewAnimator parent, int position, List<IFile> files) {
        super(parent);

		mPosition = position;
		mFiles = files;
	}
	
	@Override
	public void onClick(View v) {
		PlaybackService.launchMusicService(v.getContext(), mPosition, FileStringListUtilities.serializeFileStringList(mFiles));

        super.onClick(v);
	}
}
