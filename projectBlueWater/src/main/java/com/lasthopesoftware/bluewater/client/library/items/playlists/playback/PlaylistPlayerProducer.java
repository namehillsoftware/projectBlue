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

	public PlaylistPlayerProducer(IBufferingPlaybackQueuesProvider playbackQueuesProvider) {
		this.playbackQueuesProvider = playbackQueuesProvider;
	}

	@Override
	public IPlaylistPlayer getPlaylistPlayer(List<IFile> files, int startFilePosition, int startFileAt, boolean isCyclical) {
		final IPreparedPlaybackFileQueue playbackFileQueue = new PreparedPlaybackQueue(playbackQueuesProvider.getQueue(files, startFilePosition, isCyclical));
		return new PlaylistPlayer(playbackFileQueue, startFileAt);
	}
}
