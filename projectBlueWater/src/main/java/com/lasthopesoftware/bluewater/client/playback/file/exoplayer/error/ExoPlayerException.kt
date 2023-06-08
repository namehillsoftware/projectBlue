package com.lasthopesoftware.bluewater.client.playback.file.exoplayer.error;

import com.lasthopesoftware.bluewater.client.playback.file.error.PlaybackException;
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.ExoPlayerPlaybackHandler;


public class ExoPlayerException extends PlaybackException {
	public ExoPlayerException(ExoPlayerPlaybackHandler playbackHandler) {
		super(playbackHandler);
	}

	public ExoPlayerException(ExoPlayerPlaybackHandler playbackHandler, Throwable cause) {
		super(playbackHandler, cause);
	}
}
