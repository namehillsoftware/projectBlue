package com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.queues;

import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.IPlaybackPreparerTaskFactory;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.PositionedFileContainer;
import com.lasthopesoftware.promises.IPromise;
import com.lasthopesoftware.promises.Promise;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;

/**
 * Created by david on 9/26/16.
 */
class CyclicalBufferingPlaybackQueue implements IBufferingPlaybackPromiseQueue
{
	private final IPlaybackPreparerTaskFactory playbackPreparerTaskFactory;
	private final Queue<PositionedFileContainer> playlist;

	CyclicalBufferingPlaybackQueue(List<PositionedFileContainer> playlist, IPlaybackPreparerTaskFactory playbackPreparerTaskFactory) {
		this.playlist = new ArrayDeque<>(playlist);
		this.playbackPreparerTaskFactory = playbackPreparerTaskFactory;
	}

	@Override
	public IPromise<PositionedBufferingPlaybackHandler> getNextPreparingMediaPlayerPromise(int preparedAt) {
		if (playlist.size() == 0)
			return null;

		final PositionedFileContainer positionedFileContainer = playlist.poll();
		playlist.offer(positionedFileContainer);

		return
			new Promise<>(playbackPreparerTaskFactory.getPlaybackPreparerTask(positionedFileContainer.file, preparedAt))
				.then(handler -> new PositionedBufferingPlaybackHandler(positionedFileContainer, handler));
	}
}
