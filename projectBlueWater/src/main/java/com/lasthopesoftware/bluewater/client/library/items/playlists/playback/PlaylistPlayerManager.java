package com.lasthopesoftware.bluewater.client.library.items.playlists.playback;

import com.lasthopesoftware.bluewater.client.library.items.media.files.IFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.PositionedPlaybackFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.queues.IBufferingPlaybackQueuesProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.queues.IPreparedPlaybackFileQueue;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.queues.PreparedPlaybackQueue;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;

import io.reactivex.ObservableEmitter;
import io.reactivex.Observer;

/**
 * Created by david on 12/17/16.
 */
public class PlaylistPlayerManager implements IPlaylistPlayerManager, Closeable {

	private final IBufferingPlaybackQueuesProvider playbackQueuesProvider;
	private List<IFile> playlist;
	private PlaylistPlayer playlistPlayer;
	private Observer<? super PositionedPlaybackFile> observer;
	private ObservableEmitter<PositionedPlaybackFile> emitter;

	public PlaylistPlayerManager(IBufferingPlaybackQueuesProvider playbackQueuesProvider) {
		this.playbackQueuesProvider = playbackQueuesProvider;
	}

	@Override
	public IPlaylistPlayerManager startAsCompletable(List<IFile> playlist, int playlistStart, int fileStart) {
		this.playlist = playlist;
		final IPreparedPlaybackFileQueue playbackFileQueue = new PreparedPlaybackQueue(playbackQueuesProvider.getCompletableQueue(playlist, playlistStart));
		return getNewPlaylistPlayer(playbackFileQueue, fileStart);
	}

	@Override
	public IPlaylistPlayerManager startAsCyclical(List<IFile> playlist, int playlistStart, int fileStart) {
		this.playlist = playlist;
		final IPreparedPlaybackFileQueue playbackFileQueue = new PreparedPlaybackQueue(playbackQueuesProvider.getCyclicalQueue(playlist, playlistStart));
		return getNewPlaylistPlayer(playbackFileQueue, fileStart);
	}

	@Override
	public IPlaylistPlayerManager continueAsCompletable() {
		return this;
	}

	@Override
	public IPlaylistPlayerManager continueAsCyclical() {
		return this;
	}

	private IPlaylistPlayerManager getNewPlaylistPlayer(IPreparedPlaybackFileQueue preparedPlaybackFileQueue, int fileStart) {
		playlistPlayer = new PlaylistPlayer(preparedPlaybackFileQueue, fileStart);

		if (emitter != null) {
			try {
				playlistPlayer.subscribe(emitter);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		return this;
	}

	@Override
	public void close() throws IOException {
		if (playlistPlayer != null)
			playlistPlayer.close();
	}

	@Override
	public void pause() {
		if (playlistPlayer != null)
			playlistPlayer.pause();
	}

	@Override
	public void resume() {
		if (playlistPlayer != null)
			playlistPlayer.resume();
	}

	@Override
	public void setVolume(float volume) {
		if (playlistPlayer != null)
			playlistPlayer.setVolume(volume);
	}

	@Override
	public void cancel() {
		if (playlistPlayer != null)
			playlistPlayer.cancel();
	}

	@Override
	public void subscribe(ObservableEmitter<PositionedPlaybackFile> e) throws Exception {
		if (playlistPlayer != null)
			playlistPlayer.subscribe(e);

		emitter = e;
	}
}
