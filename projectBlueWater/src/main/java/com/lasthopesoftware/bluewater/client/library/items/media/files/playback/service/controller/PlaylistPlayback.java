package com.lasthopesoftware.bluewater.client.library.items.media.files.playback.service.controller;

import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.queues.IPreparedPlaybackFileProvider;
import com.lasthopesoftware.promises.Promise;

import java.io.IOException;

/**
 * Created by david on 11/7/16.
 */
public class PlaylistPlayback extends Promise<Void> implements IPlaylistPlayback {

	private final PlaylistPlaybackTask playlistPlaybackTask;

	public PlaylistPlayback(IPreparedPlaybackFileProvider preparedPlaybackFileProvider, int preparedPosition) {
		this(new PlaylistPlaybackTask(preparedPlaybackFileProvider, preparedPosition));
	}

	private PlaylistPlayback(PlaylistPlaybackTask playlistPlaybackTask) {
		super(playlistPlaybackTask);

		this.playlistPlaybackTask = playlistPlaybackTask;
	}

	@Override
	public void pause() {
		this.playlistPlaybackTask.pause();
	}

	@Override
	public void resume() {
		this.playlistPlaybackTask.resume();
	}

	@Override
	public void setVolume(float volume) {
		this.playlistPlaybackTask.setVolume(volume);
	}

	@Override
	public void close() throws IOException {
		this.cancel();
	}
}
