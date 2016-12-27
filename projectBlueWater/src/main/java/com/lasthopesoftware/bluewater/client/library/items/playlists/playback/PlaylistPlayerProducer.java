package com.lasthopesoftware.bluewater.client.library.items.playlists.playback;

import com.lasthopesoftware.bluewater.client.library.items.media.files.IFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.queues.IBufferingPlaybackQueuesProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.queues.IPreparedPlaybackFileQueue;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.queues.PreparedPlaybackQueue;

import java.util.List;

/**
 * Created by david on 12/17/16.
 */
public class PlaylistPlayerProducer implements IPlaylistPlayerProducer {

	private final int startFilePosition;
	private final int startFileAt;
	private final IBufferingPlaybackQueuesProvider playbackQueuesProvider;
	private final List<IFile> files;

	public PlaylistPlayerProducer(List<IFile> files, int startFilePosition, int startFileAt, IBufferingPlaybackQueuesProvider playbackQueuesProvider) {
		this.files = files;
		this.startFilePosition = startFilePosition;
		this.startFileAt = startFileAt;
		this.playbackQueuesProvider = playbackQueuesProvider;
	}

	@Override
	public IPlaylistPlayer getCompletablePlaylistPlayer() {
		final IPreparedPlaybackFileQueue playbackFileQueue = new PreparedPlaybackQueue(playbackQueuesProvider.getCompletableQueue(files, startFilePosition));
		return new PlaylistPlayer(playbackFileQueue, startFileAt);
	}

	@Override
	public IPlaylistPlayer getCyclicalPlaylistPlayer() {
		final IPreparedPlaybackFileQueue playbackFileQueue = new PreparedPlaybackQueue(playbackQueuesProvider.getCyclicalQueue(files, startFilePosition));
		return new PlaylistPlayer(playbackFileQueue, startFileAt);
	}
}
