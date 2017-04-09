package com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file;

import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;

/**
 * Created by david on 11/13/16.
 */

public class PositionedPlaybackFile extends PositionedFile {
	private final IPlaybackHandler playbackHandler;

	public PositionedPlaybackFile(int playlistPosition, IPlaybackHandler playbackHandler, ServiceFile serviceFile) {
		super(playlistPosition, serviceFile);

		this.playbackHandler = playbackHandler;
	}

	public IPlaybackHandler getPlaybackHandler() {
		return playbackHandler;
	}
}
