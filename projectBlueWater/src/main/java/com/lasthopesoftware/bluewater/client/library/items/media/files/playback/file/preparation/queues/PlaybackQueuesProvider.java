package com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.queues;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.lasthopesoftware.bluewater.client.library.items.media.files.IFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.IPlaybackPreparerTaskFactory;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.IPreparedPlaybackFileProvider;

import java.util.List;

/**
 * Created by david on 11/1/16.
 */
public class PlaybackQueuesProvider<TMediaPlayer> implements IProvidePlaybackQueues {

	private final IPlaybackPreparerTaskFactory playbackPreparerTaskFactory;

	public PlaybackQueuesProvider(IPlaybackPreparerTaskFactory playbackPreparerTaskFactory) {
		this.playbackPreparerTaskFactory = playbackPreparerTaskFactory;
	}

	@Override
	public IPreparedPlaybackFileProvider getQueue(List<IFile> playlist, int startingAt, boolean isCyclical) {
		final List<IFile> truncatedList = Stream.of(playlist).skip(startingAt - 1).collect(Collectors.toList());

		if (!isCyclical)
			return new QueuedPlaybackHandlerProvider(truncatedList, playbackPreparerTaskFactory);

		truncatedList.addAll(truncatedList.subList(0, startingAt - 1));

		return new CyclicalQueuedPlaybackProvider(truncatedList, playbackPreparerTaskFactory);
	}
}
