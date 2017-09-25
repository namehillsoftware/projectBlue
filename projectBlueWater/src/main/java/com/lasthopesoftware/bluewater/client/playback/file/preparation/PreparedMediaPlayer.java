package com.lasthopesoftware.bluewater.client.playback.file.preparation;

import com.lasthopesoftware.bluewater.client.playback.file.IPlaybackHandler;
import com.lasthopesoftware.bluewater.client.playback.file.MediaPlayerPlaybackHandler;
import com.lasthopesoftware.bluewater.client.playback.file.buffering.IBufferingPlaybackFile;

public class PreparedMediaPlayer implements IPreparedPlaybackFile {

	private final MediaPlayerPlaybackHandler mediaPlayerPlaybackHandler;

	public PreparedMediaPlayer(MediaPlayerPlaybackHandler mediaPlayerPlaybackHandler) {
		this.mediaPlayerPlaybackHandler = mediaPlayerPlaybackHandler;
	}

	@Override
	public IBufferingPlaybackFile getBufferingPlaybackFile() {
		return mediaPlayerPlaybackHandler;
	}

	@Override
	public IPlaybackHandler getPlaybackHandler() {
		return mediaPlayerPlaybackHandler;
	}
}
