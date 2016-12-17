package com.lasthopesoftware.bluewater.client.library.items.media.files.playback.service.controller;

import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.PositionedPlaybackFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.queues.IPreparedPlaybackFileQueue;
import com.lasthopesoftware.promises.Promise;

import java.io.IOException;
import java.util.Collection;

import rx.Observable;

/**
 * Created by david on 11/7/16.
 */
public class PlaylistPlayer extends Promise<Collection<PositionedPlaybackFile>> implements IPlaylistPlayer {

	private final PlaylistPlaybackTask playlistPlaybackTask;

	public PlaylistPlayer(IPreparedPlaybackFileQueue preparedPlaybackFileProvider, int preparedPosition) {
		this(new PlaylistPlaybackTask(preparedPlaybackFileProvider, preparedPosition));
	}

	private PlaylistPlayer(PlaylistPlaybackTask playlistPlaybackTask) {
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
	public Observable<PositionedPlaybackFile> observePlaybackChanges() {
		return playlistPlaybackTask.playbackChangesPublisher;
	}

	@Override
	public void close() throws IOException {
		this.cancel();
	}
}
