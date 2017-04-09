package com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.queues;

import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.PositionedFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.PositionedPlaybackFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.buffering.IBufferingPlaybackHandler;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.IPlaybackPreparer;
import com.lasthopesoftware.promises.Promise;
import com.vedsoft.futures.callables.CarelessOneParameterFunction;
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
	CarelessOneParameterFunction<PositionedBufferingPlaybackHandler, PositionedPlaybackFile>
{
	private static final int bufferingPlaybackQueueSize = 1;

	private final ReentrantReadWriteLock queueUpdateLock = new ReentrantReadWriteLock();

	private final IPlaybackPreparer playbackPreparerTaskFactory;
	private final Queue<PositionedPreparingFile> bufferingMediaPlayerPromises = new ArrayDeque<>(bufferingPlaybackQueueSize);

	private IPositionedFileQueue positionedFileQueue;

	private PositionedPreparingFile currentPreparingPlaybackHandlerPromise;

	public PreparedPlaybackQueue(IPlaybackPreparer playbackPreparerTaskFactory, IPositionedFileQueue positionedFileQueue) {
		this.playbackPreparerTaskFactory = playbackPreparerTaskFactory;
		this.positionedFileQueue = positionedFileQueue;
	}

	public PreparedPlaybackQueue updateQueue(IPositionedFileQueue newPositionedFileQueue) {
		final ReentrantReadWriteLock.WriteLock writeLock = queueUpdateLock.writeLock();
		writeLock.lock();
		try {
			final Queue<PositionedPreparingFile> newPositionedPreparingMediaPlayerPromises = new ArrayDeque<>(bufferingPlaybackQueueSize);

			while (bufferingMediaPlayerPromises.size() > 0) {
				final PositionedFile positionedFile = newPositionedFileQueue.poll();
				final PositionedPreparingFile positionedPreparingFile = bufferingMediaPlayerPromises.poll();

				if (positionedPreparingFile.positionedFile.equals(positionedFile)) {
					newPositionedPreparingMediaPlayerPromises.offer(positionedPreparingFile);
					continue;
				}

				positionedPreparingFile.bufferingPlaybackHandlerPromise.cancel();
				while (bufferingMediaPlayerPromises.size() > 0)
					bufferingMediaPlayerPromises.poll().bufferingPlaybackHandlerPromise.cancel();

				while (newPositionedPreparingMediaPlayerPromises.size() > 0)
					bufferingMediaPlayerPromises.offer(newPositionedPreparingMediaPlayerPromises.poll());

				if (positionedFile != null) {
					enqueuePositionedPreparingFile(
						new PositionedPreparingFile(
							positionedFile,
							playbackPreparerTaskFactory.promisePreparedPlaybackHandler(positionedFile.serviceFile, 0)));
				}

				break;
			}

			positionedFileQueue = newPositionedFileQueue;

			return this;
		} finally {
			writeLock.unlock();
		}
	}

	@Override
	public Promise<PositionedPlaybackFile> promiseNextPreparedPlaybackFile(int preparedAt) {
		currentPreparingPlaybackHandlerPromise =
			bufferingMediaPlayerPromises.size() > 0
				? bufferingMediaPlayerPromises.poll()
				: getNextPreparingMediaPlayerPromise(preparedAt);

		return
			currentPreparingPlaybackHandlerPromise != null
				? currentPreparingPlaybackHandlerPromise
					.promisePositionedBufferingPlaybackHandler()
					.then(this)
				: null;
	}

	private PositionedPreparingFile getNextPreparingMediaPlayerPromise(int preparedAt) {
		final ReentrantReadWriteLock.WriteLock writeLock = queueUpdateLock.writeLock();
		writeLock.lock();

		final PositionedFile positionedFile;
		try {
			positionedFile = positionedFileQueue.poll();
		} finally {
			writeLock.unlock();
		}

		if (positionedFile == null) return null;

		return
			new PositionedPreparingFile(
				positionedFile,
				playbackPreparerTaskFactory.promisePreparedPlaybackHandler(positionedFile.serviceFile, preparedAt));
	}

	@Override
	public PositionedPlaybackFile resultFrom(PositionedBufferingPlaybackHandler positionedBufferingPlaybackHandler) {
		positionedBufferingPlaybackHandler.bufferingPlaybackHandler.bufferPlaybackFile().then(VoidFunc.runCarelessly(this));

		return new PositionedPlaybackFile(positionedBufferingPlaybackHandler.positionedFile.playlistPosition, positionedBufferingPlaybackHandler.bufferingPlaybackHandler, positionedBufferingPlaybackHandler.positionedFile.serviceFile);
	}

	@Override
	public void runWith(IBufferingPlaybackHandler bufferingPlaybackHandler) {
		final ReentrantReadWriteLock.ReadLock readLock = queueUpdateLock.readLock();
		readLock.lock();
		try {
			if (bufferingMediaPlayerPromises.size() >= bufferingPlaybackQueueSize) return;
		} finally {
			readLock.unlock();
		}

		final PositionedPreparingFile nextPreparingMediaPlayerPromise = getNextPreparingMediaPlayerPromise(0);
		if (nextPreparingMediaPlayerPromise != null)
			enqueuePositionedPreparingFile(nextPreparingMediaPlayerPromise);
	}

	private void enqueuePositionedPreparingFile(PositionedPreparingFile positionedPreparingFile) {
		positionedPreparingFile.bufferingPlaybackHandlerPromise.then(handler -> new PositionedBufferingPlaybackHandler(currentPreparingPlaybackHandlerPromise.positionedFile, handler)).then(this);

		final ReentrantReadWriteLock.WriteLock writeLock = queueUpdateLock.writeLock();
		writeLock.lock();
		try {
			bufferingMediaPlayerPromises.offer(positionedPreparingFile);
		} finally {
			writeLock.unlock();
		}
	}

	@Override
	public void close() throws IOException {
		if (currentPreparingPlaybackHandlerPromise != null)
			currentPreparingPlaybackHandlerPromise.bufferingPlaybackHandlerPromise.cancel();

		final ReentrantReadWriteLock.WriteLock writeLock = queueUpdateLock.writeLock();
		writeLock.lock();
		try {
			while (bufferingMediaPlayerPromises.size() > 0)
				bufferingMediaPlayerPromises.poll().bufferingPlaybackHandlerPromise.cancel();
		} finally {
			writeLock.unlock();
		}
	}

	private static class PositionedPreparingFile {
		final PositionedFile positionedFile;
		final Promise<IBufferingPlaybackHandler> bufferingPlaybackHandlerPromise;

		private PositionedPreparingFile(PositionedFile positionedFile, Promise<IBufferingPlaybackHandler> bufferingPlaybackHandlerPromise) {
			this.positionedFile = positionedFile;
			this.bufferingPlaybackHandlerPromise = bufferingPlaybackHandlerPromise;
		}

		Promise<PositionedBufferingPlaybackHandler> promisePositionedBufferingPlaybackHandler() {
			return
				bufferingPlaybackHandlerPromise
					.then(handler -> new PositionedBufferingPlaybackHandler(positionedFile, handler));
		}
	}
}
