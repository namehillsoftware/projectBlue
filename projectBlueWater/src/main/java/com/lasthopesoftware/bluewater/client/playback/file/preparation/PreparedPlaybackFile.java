package com.lasthopesoftware.bluewater.client.playback.file.preparation;

import com.lasthopesoftware.bluewater.client.playback.file.IPlaybackHandler;
import com.lasthopesoftware.bluewater.client.playback.file.buffering.IBufferingPlaybackFile;

public class PreparedPlaybackFile {

	private final IPlaybackHandler playbackHandler;
	private final IBufferingPlaybackFile bufferingPlaybackFile;

	public PreparedPlaybackFile(IPlaybackHandler playbackHandler, IBufferingPlaybackFile bufferingPlaybackFile) {
		this.playbackHandler = playbackHandler;
		this.bufferingPlaybackFile = bufferingPlaybackFile;
	}

	public IBufferingPlaybackFile getBufferingPlaybackFile() {
		return bufferingPlaybackFile;
	}

	public IPlaybackHandler getPlaybackHandler() {
		return playbackHandler;
	}
}
