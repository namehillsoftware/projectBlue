package com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file;

import com.lasthopesoftware.bluewater.client.library.items.media.files.File;
import com.lasthopesoftware.bluewater.client.library.items.media.files.IFile;

/**
 * Created by david on 11/13/16.
 */

public class PositionedPlaybackFile extends File {
	private final int position;
	private final IPlaybackHandler playbackHandler;

	public PositionedPlaybackFile(int position, IPlaybackHandler playbackHandler, IFile file) {
		super(file.getKey());
		this.position = position;
		this.playbackHandler = playbackHandler;
	}

	public int getPosition() {
		return position;
	}

	public IPlaybackHandler getPlaybackHandler() {
		return playbackHandler;
	}
}
