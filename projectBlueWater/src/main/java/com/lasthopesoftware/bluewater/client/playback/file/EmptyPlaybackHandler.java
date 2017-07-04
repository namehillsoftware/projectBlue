package com.lasthopesoftware.bluewater.client.playback.file;

import com.lasthopesoftware.bluewater.client.playback.file.buffering.IBufferingPlaybackHandler;
import com.lasthopesoftware.messenger.promises.Promise;

import java.io.IOException;

/**
 * Created by david on 2/4/17.
 */

public class EmptyPlaybackHandler implements IBufferingPlaybackHandler {

	private final int duration;
	private float volume;

	public EmptyPlaybackHandler(int duration) {
		this.duration = duration;
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
		this.volume = volume;
	}

	@Override
	public float getVolume() {
		return this.volume;
	}

	@Override
	public int getCurrentPosition() {
		return 0;
	}

	@Override
	public int getDuration() {
		return duration;
	}

	@Override
	public Promise<IPlaybackHandler> promisePlayback() {
		return new Promise<>(this);
	}

	@Override
	public void close() throws IOException {

	}

	@Override
	public Promise<IBufferingPlaybackHandler> bufferPlaybackFile() {
		return new Promise<>(this);
	}
}
