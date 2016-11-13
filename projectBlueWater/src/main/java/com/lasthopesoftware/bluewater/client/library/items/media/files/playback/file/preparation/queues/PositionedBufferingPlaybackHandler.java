package com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.queues;

import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.buffering.IBufferingPlaybackHandler;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.PositionedFileContainer;

/**
 * Created by david on 11/6/16.
 */
class PositionedBufferingPlaybackHandler {
	final PositionedFileContainer positionedFileContainer;
	final IBufferingPlaybackHandler bufferingPlaybackHandler;

	PositionedBufferingPlaybackHandler(PositionedFileContainer positionedFileContainer, IBufferingPlaybackHandler bufferingPlaybackHandler) {
		this.positionedFileContainer = positionedFileContainer;
		this.bufferingPlaybackHandler = bufferingPlaybackHandler;
	}
}
