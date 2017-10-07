package com.lasthopesoftware.bluewater.client.playback.file.specs.fakes;

import com.lasthopesoftware.bluewater.client.playback.file.IPlaybackHandler;
import com.lasthopesoftware.bluewater.client.playback.file.buffering.IBufferingPlaybackFile;
import com.lasthopesoftware.bluewater.client.playback.file.preparation.IPreparedPlaybackFile;


public class FakePreparedPlaybackFile<PlaybackHandler extends IPlaybackHandler & IBufferingPlaybackFile> implements IPreparedPlaybackFile {

	private final PlaybackHandler fakeBufferingPlaybackHandler;

	public FakePreparedPlaybackFile(PlaybackHandler playbackHandler) {
		fakeBufferingPlaybackHandler = playbackHandler;
	}

	@Override
	public IBufferingPlaybackFile getBufferingPlaybackFile() {
		return fakeBufferingPlaybackHandler;
	}

	@Override
	public IPlaybackHandler getPlaybackHandler() {
		return fakeBufferingPlaybackHandler;
	}
}
