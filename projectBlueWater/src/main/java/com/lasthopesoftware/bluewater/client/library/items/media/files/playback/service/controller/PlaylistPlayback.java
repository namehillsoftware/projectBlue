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
public class PlaylistPlayback extends Promise<Collection<PositionedPlaybackFile>> implements IPlaylistPlayback {

	private final PlaylistPlaybackTask playlistPlaybackTask;
	private final Observable<PositionedPlaybackFile> playbackFileObservable;

	public PlaylistPlayback(IPreparedPlaybackFileQueue preparedPlaybackFileProvider, int preparedPosition) {
		this(new PlaylistPlaybackTask(preparedPlaybackFileProvider, preparedPosition));
	}

	private PlaylistPlayback(PlaylistPlaybackTask playlistPlaybackTask) {
		super(playlistPlaybackTask);

		this.playlistPlaybackTask = playlistPlaybackTask;
		playbackFileObservable = playlistPlaybackTask.playbackChangesPublisher.asObservable();
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
		return playbackFileObservable;
	}

	@Override
	public void close() throws IOException {
		this.cancel();
	}
}
