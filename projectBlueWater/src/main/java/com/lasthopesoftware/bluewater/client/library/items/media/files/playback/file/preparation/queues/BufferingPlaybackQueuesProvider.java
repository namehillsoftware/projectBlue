package com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.queues;

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
	public IBufferingPlaybackPromiseQueue getCompletableQueue(List<IFile> playlist, int startingAt) {
		return new BufferingPlaybackQueue(getTruncatedList(playlist, startingAt), playbackPreparerTaskFactory);
	}

	@Override
	public IBufferingPlaybackPromiseQueue getCyclicalQueue(List<IFile> playlist, int startingAt) {
		final List<PositionedFileContainer> truncatedList = getTruncatedList(playlist, startingAt);

		final int endingPosition = Math.max(startingAt - 1, playlist.size());
		for (int i = 0; i < endingPosition; i++)
			truncatedList.add(new PositionedFileContainer(i, playlist.get(i)));

		return new CyclicalBufferingPlaybackQueue(truncatedList, playbackPreparerTaskFactory);
	}

	private static List<PositionedFileContainer> getTruncatedList(List<IFile> playlist, int startingAt) {
		final List<PositionedFileContainer> positionedFiles = new ArrayList<>(playlist.size());

		for (int i = startingAt; i < playlist.size(); i++)
			positionedFiles.add(new PositionedFileContainer(i, playlist.get(i)));

		return positionedFiles;
	}
}
