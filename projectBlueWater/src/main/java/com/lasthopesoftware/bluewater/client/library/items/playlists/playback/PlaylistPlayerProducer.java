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

	private final IBufferingPlaybackQueuesProvider playbackQueuesProvider;
	private List<IFile> files;

	public PlaylistPlayerProducer(List<IFile> files, IBufferingPlaybackQueuesProvider playbackQueuesProvider) {
		this.files = files;
		this.playbackQueuesProvider = playbackQueuesProvider;
	}

	@Override
	public IPlaylistPlayer getCompletablePlaylistPlayer(int startFilePosition, int startFileAt) {
		final IPreparedPlaybackFileQueue playbackFileQueue = new PreparedPlaybackQueue(playbackQueuesProvider.getCompletableQueue(files, startFilePosition));
		return new PlaylistPlayer(playbackFileQueue, startFileAt);
	}

	@Override
	public IPlaylistPlayer getCyclicalPlaylistPlayer(int startFilePosition, int startFileAt) {
		final IPreparedPlaybackFileQueue playbackFileQueue = new PreparedPlaybackQueue(playbackQueuesProvider.getCyclicalQueue(files, startFilePosition));
		return new PlaylistPlayer(playbackFileQueue, startFileAt);
	}
}
