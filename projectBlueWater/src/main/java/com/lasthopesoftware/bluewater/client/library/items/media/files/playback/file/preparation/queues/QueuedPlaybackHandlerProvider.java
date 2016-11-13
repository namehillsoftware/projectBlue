package com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.queues;

import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.PositionedPlaybackFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.buffering.IBufferingPlaybackHandler;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.IPlaybackPreparerTaskFactory;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.PositionedFileContainer;
import com.lasthopesoftware.promises.IPromise;
import com.lasthopesoftware.promises.Promise;
import com.vedsoft.futures.callables.OneParameterFunction;
import com.vedsoft.futures.callables.OneParameterVoidFunction;
import com.vedsoft.futures.runnables.OneParameterAction;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;

/**
 * Created by david on 9/26/16.
 */
public class QueuedPlaybackHandlerProvider implements
	IPreparedPlaybackFileProvider,
	OneParameterAction<IBufferingPlaybackHandler>,
	OneParameterFunction<PositionedBufferingPlaybackHandler, PositionedPlaybackFile>
{
	private final Queue<PositionedFileContainer> playlist;
	private final IPlaybackPreparerTaskFactory playbackPreparerTaskFactory;

	private IPromise<PositionedBufferingPlaybackHandler> nextPreparingMediaPlayerPromise;
	private IPromise<PositionedBufferingPlaybackHandler> currentPreparingPlaybackHandlerPromise;

	public QueuedPlaybackHandlerProvider(List<PositionedFileContainer> playlist, IPlaybackPreparerTaskFactory playbackPreparerTaskFactory) {
		this.playlist = new ArrayDeque<>(playlist);
		this.playbackPreparerTaskFactory = playbackPreparerTaskFactory;
	}

	@Override
	public IPromise<PositionedPlaybackFile> promiseNextPreparedPlaybackFile(int preparedAt) {
		currentPreparingPlaybackHandlerPromise = nextPreparingMediaPlayerPromise;

		if (currentPreparingPlaybackHandlerPromise == null)
			currentPreparingPlaybackHandlerPromise = getNextPreparingMediaPlayerPromise(preparedAt);

		nextPreparingMediaPlayerPromise = null;

		return
			currentPreparingPlaybackHandlerPromise != null ?
				currentPreparingPlaybackHandlerPromise.then(this) :
				null ;
	}

	private IPromise<PositionedBufferingPlaybackHandler> getNextPreparingMediaPlayerPromise(int preparedAt) {
		if (playlist.size() == 0)
			return null;

		final PositionedFileContainer positionedFileContainer = playlist.poll();

		return
			new Promise<>(playbackPreparerTaskFactory.getPlaybackPreparerTask(positionedFileContainer.file, preparedAt))
				.then(handler -> new PositionedBufferingPlaybackHandler(positionedFileContainer, handler));
	}

	@Override
	public PositionedPlaybackFile expectedUsing(PositionedBufferingPlaybackHandler positionedBufferingPlaybackHandler) {
		positionedBufferingPlaybackHandler.bufferingPlaybackHandler.bufferPlaybackFile().then(new OneParameterVoidFunction<>(this));

		return new PositionedPlaybackFile(positionedBufferingPlaybackHandler.positionedFileContainer.playlistPosition, positionedBufferingPlaybackHandler.bufferingPlaybackHandler, positionedBufferingPlaybackHandler.positionedFileContainer.file);
	}

	@Override
	public void runWith(IBufferingPlaybackHandler bufferingPlaybackHandler) {
		if (nextPreparingMediaPlayerPromise == null)
			nextPreparingMediaPlayerPromise = getNextPreparingMediaPlayerPromise(0);
	}

	@Override
	public void close() throws IOException {
		if (currentPreparingPlaybackHandlerPromise != null)
			currentPreparingPlaybackHandlerPromise.cancel();

		if (nextPreparingMediaPlayerPromise != null)
			nextPreparingMediaPlayerPromise.cancel();
	}
}
