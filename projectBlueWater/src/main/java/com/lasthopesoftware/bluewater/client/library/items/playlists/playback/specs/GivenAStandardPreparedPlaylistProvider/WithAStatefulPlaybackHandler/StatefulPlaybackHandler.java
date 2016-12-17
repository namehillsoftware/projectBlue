package com.lasthopesoftware.bluewater.client.library.items.playlists.playback.GivenAStandardPreparedPlaylistProvider.WithAStatefulPlaybackHandler;

import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.IPlaybackHandler;
import com.lasthopesoftware.promises.IPromise;
import com.lasthopesoftware.promises.Promise;

import java.io.IOException;

/**
 * Created by david on 12/7/16.
 */

public class StatefulPlaybackHandler implements IPlaybackHandler {
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
	public void seekTo(int pos) {

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
	public IPromise<IPlaybackHandler> promisePlayback() {
		isPlaying = true;
		return new Promise<>((resolve, reject) -> {});
	}

	@Override
	public void close() throws IOException {

	}
}
