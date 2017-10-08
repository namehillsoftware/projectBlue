package com.lasthopesoftware.bluewater.client.playback.file;

import com.google.android.exoplayer2.ExoPlayer;
import com.lasthopesoftware.messenger.promises.Promise;

import java.io.IOException;

public class ExoPlayerPlaybackHandler implements IPlaybackHandler {
	private final ExoPlayer exoPlayer;

	public ExoPlayerPlaybackHandler(ExoPlayer exoPlayer) {
		this.exoPlayer = exoPlayer;
	}

	@Override
	public boolean isPlaying() {
		return false;
	}

	@Override
	public void pause() {

	}

	@Override
	public void setVolume(float volume) {

	}

	@Override
	public float getVolume() {
		return 0;
	}

	@Override
	public long getCurrentPosition() {
		return exoPlayer.getCurrentPosition();
	}

	@Override
	public long getDuration() {
		return exoPlayer.getDuration();
	}

	@Override
	public Promise<IPlaybackHandler> promisePlayback() {
		return null;
	}

	@Override
	public void close() throws IOException {

	}
}
