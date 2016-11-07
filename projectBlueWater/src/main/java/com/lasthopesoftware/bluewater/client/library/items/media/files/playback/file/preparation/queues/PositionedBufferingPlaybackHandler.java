package com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.queues;

import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.buffering.IBufferingPlaybackHandler;

/**
 * Created by david on 11/6/16.
 */
class PositionedBufferingPlaybackHandler {
	final int playlistPosition;
	final IBufferingPlaybackHandler bufferingPlaybackHandler;

	PositionedBufferingPlaybackHandler(int playlistPosition, IBufferingPlaybackHandler bufferingPlaybackHandler) {
		this.playlistPosition = playlistPosition;
		this.bufferingPlaybackHandler = bufferingPlaybackHandler;
	}
}
