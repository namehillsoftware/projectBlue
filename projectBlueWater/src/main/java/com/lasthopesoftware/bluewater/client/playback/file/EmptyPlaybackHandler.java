package com.lasthopesoftware.bluewater.client.playback.file;

import com.lasthopesoftware.bluewater.client.playback.file.buffering.IBufferingPlaybackFile;
import com.lasthopesoftware.messenger.promises.Promise;

import java.io.IOException;

public class EmptyPlaybackHandler
implements IBufferingPlaybackFile, IPlaybackHandler {

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
	public long getCurrentPosition() {
		return 0;
	}

	@Override
	public long getDuration() {
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
	public Promise<IBufferingPlaybackFile> promiseBufferedPlaybackFile() {
		return new Promise<>(this);
	}
}
