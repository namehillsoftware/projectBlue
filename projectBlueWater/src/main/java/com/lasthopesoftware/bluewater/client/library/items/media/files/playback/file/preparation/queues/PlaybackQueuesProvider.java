package com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.queues;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.lasthopesoftware.bluewater.client.library.items.media.files.IFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.IPlaybackPreparerTaskFactory;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.PositionedFileContainer;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by david on 11/1/16.
 */
public class PlaybackQueuesProvider implements IProvidePlaybackQueues {

	private final IPlaybackPreparerTaskFactory playbackPreparerTaskFactory;

	public PlaybackQueuesProvider(IPlaybackPreparerTaskFactory playbackPreparerTaskFactory) {
		this.playbackPreparerTaskFactory = playbackPreparerTaskFactory;
	}

	@Override
	public IPreparedPlaybackFileProvider getQueue(List<IFile> playlist, int startingAt, boolean isCyclical) {
		final List<PositionedFileContainer> positionedFiles = new ArrayList<>(playlist.size());

		for (int i = 0; i < playlist.size(); i++)
			positionedFiles.add(new PositionedFileContainer(i, playlist.get(i)));

		final List<PositionedFileContainer> truncatedList = Stream.of(positionedFiles).skip(startingAt - 1).collect(Collectors.toList());

		if (!isCyclical)
			return new QueuedPlaybackHandlerProvider(truncatedList, playbackPreparerTaskFactory);

		truncatedList.addAll(positionedFiles.subList(0, startingAt - 1));

		return new CyclicalQueuedPlaybackProvider(truncatedList, playbackPreparerTaskFactory);
	}
}
