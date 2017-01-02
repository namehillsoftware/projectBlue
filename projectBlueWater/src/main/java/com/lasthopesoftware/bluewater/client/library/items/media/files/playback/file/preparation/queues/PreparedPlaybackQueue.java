package com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.queues;

import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.PositionedPlaybackFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.buffering.IBufferingPlaybackHandler;
import com.lasthopesoftware.promises.IPromise;
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

	private final IBufferingPlaybackPromiseQueue nextPreparingMediaPlayerPromiseQueue;

	private IPromise<PositionedBufferingPlaybackHandler> currentPreparingPlaybackHandlerPromise;

	private final Queue<IPromise<PositionedBufferingPlaybackHandler>> bufferingMediaPlayerPromises = new ArrayDeque<>(bufferingPlaybackQueueSize);

	public PreparedPlaybackQueue(IBufferingPlaybackPromiseQueue nextPreparingMediaPlayerPromiseQueue) {
		this.nextPreparingMediaPlayerPromiseQueue = nextPreparingMediaPlayerPromiseQueue;
	}

	@Override
	public IPromise<PositionedPlaybackFile> promiseNextPreparedPlaybackFile(int preparedAt) {
		currentPreparingPlaybackHandlerPromise =
			bufferingMediaPlayerPromises.size() > 0
				? bufferingMediaPlayerPromises.poll()
				: nextPreparingMediaPlayerPromiseQueue.getNextPreparingMediaPlayerPromise(preparedAt);

		return
			currentPreparingPlaybackHandlerPromise != null ?
				currentPreparingPlaybackHandlerPromise.then(this) :
				null;
	}

	@Override
	public PositionedPlaybackFile expectedUsing(PositionedBufferingPlaybackHandler positionedBufferingPlaybackHandler) {
		positionedBufferingPlaybackHandler.bufferingPlaybackHandler.bufferPlaybackFile().then(VoidFunc.running(this));

		return new PositionedPlaybackFile(positionedBufferingPlaybackHandler.positionedFile.playlistPosition, positionedBufferingPlaybackHandler.bufferingPlaybackHandler, positionedBufferingPlaybackHandler.positionedFile.file);
	}

	@Override
	public void runWith(IBufferingPlaybackHandler bufferingPlaybackHandler) {
		while (bufferingMediaPlayerPromises.size() < bufferingPlaybackQueueSize)
			bufferingMediaPlayerPromises.offer(nextPreparingMediaPlayerPromiseQueue.getNextPreparingMediaPlayerPromise(0));
	}

	@Override
	public void close() throws IOException {
		if (currentPreparingPlaybackHandlerPromise != null)
			currentPreparingPlaybackHandlerPromise.cancel();

		while (bufferingMediaPlayerPromises.size() > 0)
			bufferingMediaPlayerPromises.poll().cancel();
	}

	@Override
	public IMutablePreparedPlaybackFileQueue updateQueue(IBufferingPlaybackPromiseQueue newBufferingPlaybackPromiseQueue) {
		return this;
	}
}
