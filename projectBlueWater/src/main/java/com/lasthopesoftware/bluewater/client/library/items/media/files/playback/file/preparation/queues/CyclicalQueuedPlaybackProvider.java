package com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.queues;

import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.buffering.IBufferingPlaybackHandler;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.IPlaybackPreparerTaskFactory;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.PositionedFileContainer;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.PositionedPlaybackHandlerContainer;
import com.lasthopesoftware.promises.IPromise;
import com.lasthopesoftware.promises.Promise;
import com.vedsoft.futures.callables.OneParameterFunction;
import com.vedsoft.futures.runnables.OneParameterAction;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;

/**
 * Created by david on 9/26/16.
 */
public class CyclicalQueuedPlaybackProvider implements
	IPreparedPlaybackFileProvider,
	OneParameterAction<IBufferingPlaybackHandler>,
	OneParameterFunction<PositionedBufferingPlaybackHandler, PositionedPlaybackHandlerContainer>
{
	private final Queue<PositionedFileContainer> playlist;
	private final IPlaybackPreparerTaskFactory playbackPreparerTaskFactory;

	private IPromise<PositionedBufferingPlaybackHandler> nextPreparingMediaPlayerPromise;
	private IPromise<PositionedBufferingPlaybackHandler> currentPreparingPlaybackHandlerPromise;

	public CyclicalQueuedPlaybackProvider(List<PositionedFileContainer> playlist, IPlaybackPreparerTaskFactory playbackPreparerTaskFactory) {
		this.playlist = new ArrayDeque<>(playlist);
		this.playbackPreparerTaskFactory = playbackPreparerTaskFactory;
	}

	@Override
	public IPromise<PositionedPlaybackHandlerContainer> promiseNextPreparedPlaybackFile(int preparedAt) {
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
		playlist.offer(positionedFileContainer);

		return
			new Promise<>(playbackPreparerTaskFactory.getPlaybackPreparerTask(positionedFileContainer.file, preparedAt))
				.then(handler -> { return new PositionedBufferingPlaybackHandler(positionedFileContainer.playlistPosition, handler ); });
	}


	@Override
	public PositionedPlaybackHandlerContainer expectedUsing(PositionedBufferingPlaybackHandler positionedBufferingPlaybackHandler) {
		positionedBufferingPlaybackHandler.bufferingPlaybackHandler.bufferPlaybackFile().then(this);

		return new PositionedPlaybackHandlerContainer(positionedBufferingPlaybackHandler.playlistPosition, positionedBufferingPlaybackHandler.bufferingPlaybackHandler);
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
