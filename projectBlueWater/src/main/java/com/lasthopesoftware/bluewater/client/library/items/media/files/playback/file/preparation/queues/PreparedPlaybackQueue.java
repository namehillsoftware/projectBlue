package com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.queues;

import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.PositionedPlaybackFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.buffering.IBufferingPlaybackHandler;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.IPlaybackPreparerTaskFactory;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.PositionedFile;
import com.lasthopesoftware.promises.IPromise;
import com.lasthopesoftware.promises.Promise;
import com.vedsoft.futures.callables.OneParameterFunction;
import com.vedsoft.futures.callables.VoidFunc;
import com.vedsoft.futures.runnables.OneParameterAction;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Queue;

/**
 * Created by david on 9/26/16.
 */
public class PreparedPlaybackQueue implements
	IMutablePreparedPlaybackFileQueue,
	OneParameterAction<IBufferingPlaybackHandler>,
	OneParameterFunction<PositionedBufferingPlaybackHandler, PositionedPlaybackFile>
{
	private static final int bufferingPlaybackQueueSize = 1;

	private final IPlaybackPreparerTaskFactory playbackPreparerTaskFactory;
	private final IPositionedFileQueue nextPreparingMediaPlayerPromiseQueue;

	private IPromise<PositionedBufferingPlaybackHandler> currentPreparingPlaybackHandlerPromise;

	private final Queue<IPromise<PositionedBufferingPlaybackHandler>> bufferingMediaPlayerPromises = new ArrayDeque<>(bufferingPlaybackQueueSize);

	public PreparedPlaybackQueue(IPlaybackPreparerTaskFactory playbackPreparerTaskFactory, IPositionedFileQueue nextPreparingMediaPlayerPromiseQueue) {
		this.playbackPreparerTaskFactory = playbackPreparerTaskFactory;
		this.nextPreparingMediaPlayerPromiseQueue = nextPreparingMediaPlayerPromiseQueue;
	}

	@Override
	public IPromise<PositionedPlaybackFile> promiseNextPreparedPlaybackFile(int preparedAt) {
		currentPreparingPlaybackHandlerPromise =
			bufferingMediaPlayerPromises.size() > 0
				? bufferingMediaPlayerPromises.poll()
				: getNextPreparingMediaPlayerPromise(preparedAt);

		return
			currentPreparingPlaybackHandlerPromise != null ?
				currentPreparingPlaybackHandlerPromise.then(this) :
				null;
	}

	private IPromise<PositionedBufferingPlaybackHandler> getNextPreparingMediaPlayerPromise(int preparedAt) {
		final PositionedFile positionedFile = this.nextPreparingMediaPlayerPromiseQueue.poll();

		if (positionedFile == null) return null;

		return
			new Promise<>(playbackPreparerTaskFactory.getPlaybackPreparerTask(positionedFile.file, preparedAt))
				.then(handler -> new PositionedBufferingPlaybackHandler(positionedFile, handler));
	}

	@Override
	public PositionedPlaybackFile expectedUsing(PositionedBufferingPlaybackHandler positionedBufferingPlaybackHandler) {
		positionedBufferingPlaybackHandler.bufferingPlaybackHandler.bufferPlaybackFile().then(VoidFunc.running(this));

		return new PositionedPlaybackFile(positionedBufferingPlaybackHandler.positionedFile.playlistPosition, positionedBufferingPlaybackHandler.bufferingPlaybackHandler, positionedBufferingPlaybackHandler.positionedFile.file);
	}

	@Override
	public synchronized void runWith(IBufferingPlaybackHandler bufferingPlaybackHandler) {
		if (bufferingMediaPlayerPromises.size() >= bufferingPlaybackQueueSize) return;

		final IPromise<PositionedBufferingPlaybackHandler> nextPreparingMediaPlayerPromise = getNextPreparingMediaPlayerPromise(0);
		if (nextPreparingMediaPlayerPromise == null) return;

		nextPreparingMediaPlayerPromise.then(this);

		bufferingMediaPlayerPromises.offer(nextPreparingMediaPlayerPromise);
	}

	@Override
	public void close() throws IOException {
		if (currentPreparingPlaybackHandlerPromise != null)
			currentPreparingPlaybackHandlerPromise.cancel();

		while (bufferingMediaPlayerPromises.size() > 0)
			bufferingMediaPlayerPromises.poll().cancel();
	}

	@Override
	public IMutablePreparedPlaybackFileQueue updateQueue(IPositionedFileQueue newBufferingPlaybackPromiseQueue) {
		return this;
	}
}
