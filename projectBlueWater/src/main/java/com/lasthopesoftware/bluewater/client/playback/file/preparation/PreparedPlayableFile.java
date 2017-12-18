package com.lasthopesoftware.bluewater.client.playback.file.preparation;

import com.lasthopesoftware.bluewater.client.playback.file.PlayableFile;
import com.lasthopesoftware.bluewater.client.playback.file.buffering.IBufferingPlaybackFile;

public class PreparedPlayableFile {

	private final PlayableFile playbackHandler;
	private final IBufferingPlaybackFile bufferingPlaybackFile;

	public PreparedPlayableFile(PlayableFile playbackHandler, IBufferingPlaybackFile bufferingPlaybackFile) {
		this.playbackHandler = playbackHandler;
		this.bufferingPlaybackFile = bufferingPlaybackFile;
	}

	public IBufferingPlaybackFile getBufferingPlaybackFile() {
		return bufferingPlaybackFile;
	}

	public PlayableFile getPlaybackHandler() {
		return playbackHandler;
	}
}
