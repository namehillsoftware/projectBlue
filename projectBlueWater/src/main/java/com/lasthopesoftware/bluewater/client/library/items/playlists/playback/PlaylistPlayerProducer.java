package com.lasthopesoftware.bluewater.client.library.items.playlists.playback;

import com.lasthopesoftware.bluewater.client.library.items.media.files.IFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.PositionedPlaybackFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.queues.IBufferingPlaybackQueuesProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.queues.IPreparedPlaybackFileQueue;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.queues.PreparedPlaybackQueue;

import java.util.List;

import rx.subjects.PublishSubject;

/**
 * Created by david on 12/17/16.
 */
public class PlaylistPlayerProducer implements IPlaylistPlayerProducer {

	private final IBufferingPlaybackQueuesProvider playbackQueuesProvider;
	private PublishSubject<PositionedPlaybackFile> playbackChangesPublisher;

	public PlaylistPlayerProducer(IBufferingPlaybackQueuesProvider playbackQueuesProvider, PublishSubject<PositionedPlaybackFile> playbackChangesPublisher) {
		this.playbackQueuesProvider = playbackQueuesProvider;
		this.playbackChangesPublisher = playbackChangesPublisher;
	}

	@Override
	public IPlaylistPlayer getPlaylistPlayer(List<IFile> files, int startFilePosition, int startFileAt, boolean isCyclical) {
		final IPreparedPlaybackFileQueue playbackFileQueue = new PreparedPlaybackQueue(playbackQueuesProvider.getQueue(files, startFilePosition, isCyclical));
		return new PlaylistPlayer(playbackFileQueue, startFilePosition, playbackChangesPublisher);
	}
}
