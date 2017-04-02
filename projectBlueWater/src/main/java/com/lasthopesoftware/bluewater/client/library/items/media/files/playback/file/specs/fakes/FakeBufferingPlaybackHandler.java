package com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.specs.fakes;

import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.IPlaybackHandler;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.buffering.IBufferingPlaybackHandler;
import com.lasthopesoftware.promises.Promise;

import java.io.IOException;

/**
 * Created by david on 3/8/17.
 */

public class FakeBufferingPlaybackHandler implements IBufferingPlaybackHandler {
	private boolean isPlaying;
	private float volume;

	@Override
	public boolean isPlaying() {
		return isPlaying;
	}

	@Override
	public void pause() {
		isPlaying = false;
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
		return 0;
	}

	@Override
	public Promise<IPlaybackHandler> promisePlayback() {
		isPlaying = true;
		return new Promise<>((resolve, reject) -> {});
	}

	@Override
	public void close() throws IOException {

	}

	@Override
	public Promise<IBufferingPlaybackHandler> bufferPlaybackFile() {
		return new Promise<>(this);
	}
}
