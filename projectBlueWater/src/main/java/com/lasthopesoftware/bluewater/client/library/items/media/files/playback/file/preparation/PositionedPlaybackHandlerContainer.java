package com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation;

import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.IPlaybackHandler;

/**
 * Created by david on 11/6/16.
 */

public class PositionedPlaybackHandlerContainer {
	public final int playlistPosition;
	public final IPlaybackHandler playbackHandler;

	public PositionedPlaybackHandlerContainer(int playlistPosition, IPlaybackHandler playbackHandler) {
		this.playlistPosition = playlistPosition;
		this.playbackHandler = playbackHandler;
	}
}
