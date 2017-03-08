package com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.specs.fakes;

import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.IPlaybackHandler;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.buffering.IBufferingPlaybackHandler;
import com.lasthopesoftware.promises.IPromise;

import java.io.IOException;

/**
 * Created by david on 3/8/17.
 */

public class FakeBufferingPlaybackHandler implements IBufferingPlaybackHandler {
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
	public int getCurrentPosition() {
		return 0;
	}

	@Override
	public int getDuration() {
		return 0;
	}

	@Override
	public IPromise<IPlaybackHandler> promisePlayback() {
		return null;
	}

	@Override
	public IPromise<IBufferingPlaybackHandler> bufferPlaybackFile() {
		return null;
	}

	@Override
	public void close() throws IOException {

	}
}
