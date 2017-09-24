package com.lasthopesoftware.bluewater.client.playback.queues;

import com.lasthopesoftware.bluewater.client.playback.file.PositionedFile;
import com.lasthopesoftware.bluewater.client.playback.file.buffering.IBufferingPlaybackHandler;

class PositionedBufferingPlaybackHandler {
	final PositionedFile positionedFile;
	final IBufferingPlaybackHandler bufferingPlaybackHandler;

	PositionedBufferingPlaybackHandler(PositionedFile positionedFile, IBufferingPlaybackHandler bufferingPlaybackHandler) {
		this.positionedFile = positionedFile;
		this.bufferingPlaybackHandler = bufferingPlaybackHandler;
	}

	boolean isEmpty() {
		return bufferingPlaybackHandler == null;
	}

	public static PositionedBufferingPlaybackHandler emptyHandler(PositionedFile positionedFile) {
		return new PositionedBufferingPlaybackHandler(positionedFile, null);
	}
}
