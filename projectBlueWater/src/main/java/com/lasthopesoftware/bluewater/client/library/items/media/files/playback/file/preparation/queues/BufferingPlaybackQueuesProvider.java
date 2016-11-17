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
public class BufferingPlaybackQueuesProvider implements IBufferingPlaybackQueuesProvider {

	private final IPlaybackPreparerTaskFactory playbackPreparerTaskFactory;

	public BufferingPlaybackQueuesProvider(IPlaybackPreparerTaskFactory playbackPreparerTaskFactory) {
		this.playbackPreparerTaskFactory = playbackPreparerTaskFactory;
	}

	@Override
	public IBufferingPlaybackPromiseQueue getQueue(List<IFile> playlist, int startingAt, boolean isCyclical) {
		final List<PositionedFileContainer> positionedFiles = new ArrayList<>(playlist.size());

		for (int i = 0; i < playlist.size(); i++)
			positionedFiles.add(new PositionedFileContainer(i, playlist.get(i)));

		final List<PositionedFileContainer> truncatedList = Stream.of(positionedFiles).skip(startingAt - 1).collect(Collectors.toList());

		if (!isCyclical)
			return new BufferingPlaybackQueue(truncatedList, playbackPreparerTaskFactory);

		truncatedList.addAll(positionedFiles.subList(0, Math.max(startingAt - 1, playlist.size())));

		return new CyclicalBufferingPlaybackQueue(truncatedList, playbackPreparerTaskFactory);
	}
}
