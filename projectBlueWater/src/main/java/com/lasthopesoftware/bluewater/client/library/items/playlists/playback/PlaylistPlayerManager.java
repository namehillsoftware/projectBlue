package com.lasthopesoftware.bluewater.client.library.items.playlists.playback;

import com.lasthopesoftware.bluewater.client.library.items.media.files.IFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.queues.IBufferingPlaybackQueuesProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.queues.IPreparedPlaybackFileQueue;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.queues.PreparedPlaybackQueue;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;

/**
 * Created by david on 12/17/16.
 */
public class PlaylistPlayerManager implements IPlaylistPlayerManager, Closeable {

	private final IBufferingPlaybackQueuesProvider playbackQueuesProvider;
	private List<IFile> files;
	private PlaylistPlayer playlistPlayer;

	public PlaylistPlayerManager(IBufferingPlaybackQueuesProvider playbackQueuesProvider) {
		this.playbackQueuesProvider = playbackQueuesProvider;
	}

	@Override
	public IPlaylistPlayer startAsCompletable(List<IFile> playlist, int playlistStart, int fileStart) {
		return null;
	}

	@Override
	public IPlaylistPlayer startAsCyclical(List<IFile> playlist, int playlistStart, int fileStart) {
		return null;
	}

	@Override
	public IPlaylistPlayer continueAsCompletable() {
		final IPreparedPlaybackFileQueue playbackFileQueue = new PreparedPlaybackQueue(playbackQueuesProvider.getCompletableQueue(files, startFilePosition));
		return new PlaylistPlayer(playbackFileQueue, startFileAt);
	}

	@Override
	public IPlaylistPlayer continueAsCyclical() {
		final IPreparedPlaybackFileQueue playbackFileQueue = new PreparedPlaybackQueue(playbackQueuesProvider.getCyclicalQueue(files, startFilePosition));
		playlistPlayer = new PlaylistPlayer(playbackFileQueue, startFileAt);
		return playlistPlayer;
	}

	@Override
	public void close() throws IOException {
		if (playlistPlayer != null)
			playlistPlayer.close();
	}
}
