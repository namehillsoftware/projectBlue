package com.lasthopesoftware.bluewater.client.playback.file.specs.fakes;

import com.lasthopesoftware.bluewater.client.playback.file.IPlaybackHandler;
import com.lasthopesoftware.bluewater.client.playback.file.buffering.IBufferingPlaybackFile;
import com.lasthopesoftware.bluewater.client.playback.file.preparation.PreparedPlaybackFile;


public class FakePreparedPlaybackFile<PlaybackHandler extends IPlaybackHandler & IBufferingPlaybackFile> extends PreparedPlaybackFile {

	public FakePreparedPlaybackFile(PlaybackHandler playbackHandler) {
		super(playbackHandler, playbackHandler);
	}
}
