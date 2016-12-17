package com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.queues;

import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.PositionedPlaybackFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.buffering.IBufferingPlaybackHandler;
import com.lasthopesoftware.promises.IPromise;
import com.vedsoft.futures.callables.OneParameterFunction;
import com.vedsoft.futures.callables.VoidFunc;
import com.vedsoft.futures.runnables.OneParameterAction;

import java.io.IOException;

/**
 * Created by david on 9/26/16.
 */
public class PreparedPlaybackQueue implements
	IPreparedPlaybackFileQueue,
	OneParameterAction<IBufferingPlaybackHandler>,
	OneParameterFunction<PositionedBufferingPlaybackHandler, PositionedPlaybackFile>
{
	private final IBufferingPlaybackPromiseQueue nextPreparingMediaPlayerPromiseQueue;

	private IPromise<PositionedBufferingPlaybackHandler> nextPreparingMediaPlayerPromise;
	private IPromise<PositionedBufferingPlaybackHandler> currentPreparingPlaybackHandlerPromise;

	public PreparedPlaybackQueue(IBufferingPlaybackPromiseQueue nextPreparingMediaPlayerPromiseQueue) {
		this.nextPreparingMediaPlayerPromiseQueue = nextPreparingMediaPlayerPromiseQueue;
	}

	@Override
	public IPromise<PositionedPlaybackFile> promiseNextPreparedPlaybackFile(int preparedAt) {
		currentPreparingPlaybackHandlerPromise = nextPreparingMediaPlayerPromise;

		if (currentPreparingPlaybackHandlerPromise == null)
			currentPreparingPlaybackHandlerPromise = nextPreparingMediaPlayerPromiseQueue.getNextPreparingMediaPlayerPromise(preparedAt);

		nextPreparingMediaPlayerPromise = null;

		return
			currentPreparingPlaybackHandlerPromise != null ?
				currentPreparingPlaybackHandlerPromise.then(this) :
				null;
	}

	@Override
	public PositionedPlaybackFile expectedUsing(PositionedBufferingPlaybackHandler positionedBufferingPlaybackHandler) {
		positionedBufferingPlaybackHandler.bufferingPlaybackHandler.bufferPlaybackFile().then(VoidFunc.running(this));

		return new PositionedPlaybackFile(positionedBufferingPlaybackHandler.positionedFileContainer.playlistPosition, positionedBufferingPlaybackHandler.bufferingPlaybackHandler, positionedBufferingPlaybackHandler.positionedFileContainer.file);
	}

	@Override
	public void runWith(IBufferingPlaybackHandler bufferingPlaybackHandler) {
		if (nextPreparingMediaPlayerPromise == null)
			nextPreparingMediaPlayerPromise = nextPreparingMediaPlayerPromiseQueue.getNextPreparingMediaPlayerPromise(0);
	}

	@Override
	public void close() throws IOException {
		if (currentPreparingPlaybackHandlerPromise != null)
			currentPreparingPlaybackHandlerPromise.cancel();

		if (nextPreparingMediaPlayerPromise != null)
			nextPreparingMediaPlayerPromise.cancel();
	}
}
