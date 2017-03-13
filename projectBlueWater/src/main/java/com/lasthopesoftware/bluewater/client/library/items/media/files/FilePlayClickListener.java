package com.lasthopesoftware.bluewater.client.library.items.media.files;

import android.content.Context;
import android.view.View;

import com.lasthopesoftware.bluewater.client.library.items.media.files.access.stringlist.FileStringListUtilities;
import com.lasthopesoftware.bluewater.client.library.items.menu.NotifyOnFlipViewAnimator;
import com.lasthopesoftware.bluewater.client.library.items.menu.handlers.AbstractMenuClickHandler;
import com.lasthopesoftware.bluewater.client.playback.service.PlaybackService;
import com.lasthopesoftware.bluewater.shared.promises.resolutions.Dispatch;
import com.vedsoft.futures.callables.VoidFunc;

import java.util.List;

public class FilePlayClickListener extends AbstractMenuClickHandler {
	private final List<IFile> files;
	private final int position;
	
	public FilePlayClickListener(NotifyOnFlipViewAnimator parent, int position, List<IFile> files) {
        super(parent);

		this.position = position;
		this.files = files;
	}
	
	@Override
	public void onClick(View v) {
		final Context context = v.getContext();

		FileStringListUtilities
			.promiseSerializedFileStringList(files)
			.then(Dispatch.toContext(VoidFunc.runCarelessly(fileStringList -> PlaybackService.launchMusicService(context, position, FileStringListUtilities.serializeFileStringList(files))), context));

        super.onClick(v);
	}
}
