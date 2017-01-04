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
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Created by david on 9/26/16.
 */
public class PreparedPlaybackQueue implements
	IPreparedPlaybackFileQueue,
	OneParameterAction<IBufferingPlaybackHandler>,
	OneParameterFunction<PositionedBufferingPlaybackHandler, PositionedPlaybackFile>
{
	private static final int bufferingPlaybackQueueSize = 1;

	private final ReentrantReadWriteLock queueUpdateLock = new ReentrantReadWriteLock();

	private final IPlaybackPreparerTaskFactory playbackPreparerTaskFactory;
	private final Queue<PositionedPreparingFile> bufferingMediaPlayerPromises = new ArrayDeque<>(bufferingPlaybackQueueSize);

	private IPositionedFileQueue positionedFileQueue;

	private PositionedPreparingFile currentPreparingPlaybackHandlerPromise;

	public PreparedPlaybackQueue(IPlaybackPreparerTaskFactory playbackPreparerTaskFactory, IPositionedFileQueue positionedFileQueue) {
		this.playbackPreparerTaskFactory = playbackPreparerTaskFactory;
		this.positionedFileQueue = positionedFileQueue;
	}

	public PreparedPlaybackQueue updateQueue(IPositionedFileQueue newPositionedFileQueue) {
		queueUpdateLock.writeLock().lock();
		try {
			if (bufferingMediaPlayerPromises.size() == 0) {
				this.positionedFileQueue = newPositionedFileQueue;
				return this;
			}

			final Queue<PositionedPreparingFile> newPositionedPreparingMediaPlayerPromises = new ArrayDeque<>(bufferingPlaybackQueueSize);

			while (bufferingMediaPlayerPromises.size() > 0) {
				final PositionedFile positionedFile = newPositionedFileQueue.poll();
				final PositionedPreparingFile positionedPreparingFile = bufferingMediaPlayerPromises.poll();

				if (positionedFile == null) break;

				if (positionedFile.equals(positionedPreparingFile.positionedFile)) {
					newPositionedPreparingMediaPlayerPromises.offer(positionedPreparingFile);
					continue;
				}

				while (bufferingMediaPlayerPromises.size() > 0)
					bufferingMediaPlayerPromises.poll().positionedBufferingPlaybackHandlerPromise.cancel();

				enqueuePositionedPreparingFile(
					new PositionedPreparingFile(
						positionedFile,
						new Promise<>(playbackPreparerTaskFactory.getPlaybackPreparerTask(positionedFile.file, 0))
							.then(handler -> new PositionedBufferingPlaybackHandler(positionedFile, handler))));
			}

			while (newPositionedPreparingMediaPlayerPromises.size() > 0)
				bufferingMediaPlayerPromises.offer(newPositionedPreparingMediaPlayerPromises.poll());

			this.positionedFileQueue = newPositionedFileQueue;

			return this;
		} finally {
			queueUpdateLock.writeLock().unlock();
		}
	}

	@Override
	public IPromise<PositionedPlaybackFile> promiseNextPreparedPlaybackFile(int preparedAt) {
		currentPreparingPlaybackHandlerPromise =
			bufferingMediaPlayerPromises.size() > 0
				? bufferingMediaPlayerPromises.poll()
				: getNextPreparingMediaPlayerPromise(preparedAt);

		return
			currentPreparingPlaybackHandlerPromise != null ?
				currentPreparingPlaybackHandlerPromise.positionedBufferingPlaybackHandlerPromise.then(this) :
				null;
	}

	private PositionedPreparingFile getNextPreparingMediaPlayerPromise(int preparedAt) {
		final PositionedFile positionedFile = positionedFileQueue.poll();

		if (positionedFile == null) return null;

		return
			new PositionedPreparingFile(
				positionedFile,
				new Promise<>(playbackPreparerTaskFactory.getPlaybackPreparerTask(positionedFile.file, preparedAt))
					.then(handler -> new PositionedBufferingPlaybackHandler(positionedFile, handler)));
	}

	@Override
	public PositionedPlaybackFile expectedUsing(PositionedBufferingPlaybackHandler positionedBufferingPlaybackHandler) {
		positionedBufferingPlaybackHandler.bufferingPlaybackHandler.bufferPlaybackFile().then(VoidFunc.running(this));

		return new PositionedPlaybackFile(positionedBufferingPlaybackHandler.positionedFile.playlistPosition, positionedBufferingPlaybackHandler.bufferingPlaybackHandler, positionedBufferingPlaybackHandler.positionedFile.file);
	}

	@Override
	public synchronized void runWith(IBufferingPlaybackHandler bufferingPlaybackHandler) {
		if (bufferingMediaPlayerPromises.size() >= bufferingPlaybackQueueSize) return;

		final PositionedPreparingFile nextPreparingMediaPlayerPromise = getNextPreparingMediaPlayerPromise(0);
		if (nextPreparingMediaPlayerPromise != null)
			enqueuePositionedPreparingFile(nextPreparingMediaPlayerPromise);
	}

	private void enqueuePositionedPreparingFile(PositionedPreparingFile positionedPreparingFile) {
		positionedPreparingFile.positionedBufferingPlaybackHandlerPromise.then(this);

		bufferingMediaPlayerPromises.offer(positionedPreparingFile);
	}

	@Override
	public void close() throws IOException {
		if (currentPreparingPlaybackHandlerPromise != null)
			currentPreparingPlaybackHandlerPromise.positionedBufferingPlaybackHandlerPromise.cancel();

		while (bufferingMediaPlayerPromises.size() > 0)
			bufferingMediaPlayerPromises.poll().positionedBufferingPlaybackHandlerPromise.cancel();
	}

	private static class PositionedPreparingFile {
		final PositionedFile positionedFile;
		final IPromise<PositionedBufferingPlaybackHandler> positionedBufferingPlaybackHandlerPromise;

		private PositionedPreparingFile(PositionedFile positionedFile, IPromise<PositionedBufferingPlaybackHandler> positionedBufferingPlaybackHandlerPromise) {
			this.positionedFile = positionedFile;
			this.positionedBufferingPlaybackHandlerPromise = positionedBufferingPlaybackHandlerPromise;
		}
	}
}
