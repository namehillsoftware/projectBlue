package com.lasthopesoftware.bluewater.client.playback.file;

import com.google.android.exoplayer2.SimpleExoPlayer;
import com.lasthopesoftware.messenger.promises.Promise;

import java.io.IOException;

public class ExoPlayerPlaybackHandler implements IPlaybackHandler {
	private final SimpleExoPlayer exoPlayer;

	public ExoPlayerPlaybackHandler(SimpleExoPlayer exoPlayer) {
		this.exoPlayer = exoPlayer;
	}

	@Override
	public boolean isPlaying() {
		return false;
	}

	@Override
	public void pause() {
		exoPlayer.setPlayWhenReady(false);
	}

	@Override
	public void setVolume(float volume) {
		exoPlayer.setVolume(volume);
	}

	@Override
	public float getVolume() {
		return exoPlayer.getVolume();
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
		exoPlayer.setPlayWhenReady(true);
		return null;
	}

	@Override
	public void close() throws IOException {
		exoPlayer.release();
	}
}
