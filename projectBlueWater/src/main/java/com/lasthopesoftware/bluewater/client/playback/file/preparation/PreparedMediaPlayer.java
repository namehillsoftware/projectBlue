package com.lasthopesoftware.bluewater.client.playback.file.preparation;

import com.lasthopesoftware.bluewater.client.playback.file.IPlaybackHandler;
import com.lasthopesoftware.bluewater.client.playback.file.MediaPlayerPlaybackHandler;
import com.lasthopesoftware.bluewater.client.playback.file.buffering.BufferingMediaPlayerFile;
import com.lasthopesoftware.bluewater.client.playback.file.buffering.IBufferingPlaybackFile;

class PreparedMediaPlayer implements IPreparedPlaybackFile {

	private final MediaPlayerPlaybackHandler mediaPlayerPlaybackHandler;
	private final BufferingMediaPlayerFile bufferingMediaPlayerFile;

	PreparedMediaPlayer(MediaPlayerPlaybackHandler mediaPlayerPlaybackHandler, BufferingMediaPlayerFile bufferingMediaPlayerFile) {
		this.mediaPlayerPlaybackHandler = mediaPlayerPlaybackHandler;
		this.bufferingMediaPlayerFile = bufferingMediaPlayerFile;
	}

	@Override
	public IBufferingPlaybackFile getBufferingPlaybackFile() {
		return bufferingMediaPlayerFile;
	}

	@Override
	public IPlaybackHandler getPlaybackHandler() {
		return mediaPlayerPlaybackHandler;
	}
}
