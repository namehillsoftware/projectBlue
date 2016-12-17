package com.lasthopesoftware.bluewater.client.library.items.playlists.playback;

import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.PositionedPlaybackFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.queues.IPreparedPlaybackFileQueue;
import com.lasthopesoftware.promises.Promise;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Collection;

import rx.subjects.PublishSubject;

/**
 * Created by david on 11/7/16.
 */
public class PlaylistPlayer extends Promise<Collection<PositionedPlaybackFile>> implements IPlaylistPlayer {

	private final PlaylistPlayerTask playlistPlayerTask;

	public PlaylistPlayer(@NotNull IPreparedPlaybackFileQueue preparedPlaybackFileProvider, int preparedPosition, @Nullable PublishSubject<PositionedPlaybackFile> playbackChangesPublisher) {
		this(new PlaylistPlayerTask(preparedPlaybackFileProvider, preparedPosition, playbackChangesPublisher));
	}

	private PlaylistPlayer(PlaylistPlayerTask playlistPlayerTask) {
		super(playlistPlayerTask);

		this.playlistPlayerTask = playlistPlayerTask;
	}

	@Override
	public void pause() {
		this.playlistPlayerTask.pause();
	}

	@Override
	public void resume() {
		this.playlistPlayerTask.resume();
	}

	@Override
	public void setVolume(float volume) {
		this.playlistPlayerTask.setVolume(volume);
	}

	@Override
	public void close() throws IOException {
		this.cancel();
	}
}
