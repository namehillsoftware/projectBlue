package com.lasthopesoftware.bluewater.client.library.items.playlists.playback;

import com.lasthopesoftware.bluewater.client.library.items.media.files.IFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.PositionedPlaybackFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.queues.IBufferingPlaybackQueuesProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.queues.IPreparedPlaybackFileQueue;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.queues.PreparedPlaybackQueue;

import java.io.IOException;
import java.util.List;

import io.reactivex.Observer;
import io.reactivex.Single;

/**
 * Created by david on 12/17/16.
 */
public class PlaylistPlayerManager implements IPlaylistPlayerManager {

	private final IBufferingPlaybackQueuesProvider playbackQueuesProvider;
	private List<IFile> files;
	private IPlaylistPlayer playlistPlayer;

	public PlaylistPlayerManager(IBufferingPlaybackQueuesProvider playbackQueuesProvider) {
		this.playbackQueuesProvider = playbackQueuesProvider;
	}

	@Override
	public IPlaylistPlayerManager startAsCompletable(List<IFile> playlist, int playlistStart, int fileStart) {
		return null;
	}

	@Override
	public IPlaylistPlayerManager startAsCyclical(List<IFile> playlist, int playlistStart, int fileStart) {
		return null;
	}

	@Override
	public IPlaylistPlayerManager continueAsCompletable() {
		final IPreparedPlaybackFileQueue playbackFileQueue = new PreparedPlaybackQueue(playbackQueuesProvider.getCompletableQueue(files, startFilePosition));
		return new PlaylistPlayer(playbackFileQueue, startFileAt);
	}

	@Override
	public IPlaylistPlayerManager continueAsCyclical() {
		final IPreparedPlaybackFileQueue playbackFileQueue = new PreparedPlaybackQueue(playbackQueuesProvider.getCyclicalQueue(files, startFilePosition));
		playlistPlayer = new PlaylistPlayer(playbackFileQueue, startFileAt);
		return this;
	}

	@Override
	public void pause() {

	}

	@Override
	public void resume() {

	}

	@Override
	public void setVolume(float volume) {

	}

	@Override
	public void cancel() {

	}

	@Override
	public Single<List<PositionedPlaybackFile>> toList() {
		return null;
	}

	@Override
	public void subscribe(Observer<? super PositionedPlaybackFile> observer) {

	}

	@Override
	public void close() throws IOException {

	}
}
