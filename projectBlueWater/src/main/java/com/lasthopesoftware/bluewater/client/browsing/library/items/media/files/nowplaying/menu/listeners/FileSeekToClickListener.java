package com.lasthopesoftware.bluewater.client.browsing.library.items.media.files.nowplaying.menu.listeners;

import android.view.View;

import com.lasthopesoftware.bluewater.client.browsing.library.items.menu.NotifyOnFlipViewAnimator;
import com.lasthopesoftware.bluewater.client.browsing.library.items.menu.handlers.AbstractMenuClickHandler;
import com.lasthopesoftware.bluewater.client.playback.service.PlaybackService;

/**
 * Created by david on 1/22/17.
 */

public class FileSeekToClickListener extends AbstractMenuClickHandler {
	private final int position;

	public FileSeekToClickListener(NotifyOnFlipViewAnimator parent, int position) {
		super(parent);

		this.position = position;
	}

	@Override
	public void onClick(View v) {
		PlaybackService.seekTo(v.getContext(), position);

		super.onClick(v);
	}
}
