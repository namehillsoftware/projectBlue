package com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.queues;

import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.PositionedPlaybackFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.buffering.IBufferingPlaybackHandler;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.IPlaybackPreparerTaskFactory;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.PositionedFile;
import com.lasthopesoftware.promises.IPromise;
import com.lasthopesoftware.promises.Promise;
import com.vedsoft.futures.callables.CarelessOneParameterFunction;
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
	CarelessOneParameterFunction<PositionedBufferingPlaybackHandler, PositionedPlaybackFile>
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

				positionedPreparingFile.positionedBufferingPlaybackHandlerPromise.cancel();
				while (bufferingMediaPlayerPromises.size() > 0)
					bufferingMediaPlayerPromises.poll().positionedBufferingPlaybackHandlerPromise.cancel();

				while (newPositionedPreparingMediaPlayerPromises.size() > 0)
					bufferingMediaPlayerPromises.offer(newPositionedPreparingMediaPlayerPromises.poll());

				if (positionedFile != null) {
					enqueuePositionedPreparingFile(
						new PositionedPreparingFile(
							positionedFile,
							new Promise<>(playbackPreparerTaskFactory.getPlaybackPreparerTask(positionedFile.file, 0))
								.then(handler -> new PositionedBufferingPlaybackHandler(positionedFile, handler))));
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
				new Promise<>(playbackPreparerTaskFactory.getPlaybackPreparerTask(positionedFile.file, preparedAt))
					.then(handler -> new PositionedBufferingPlaybackHandler(positionedFile, handler)));
	}

	@Override
	public PositionedPlaybackFile resultFrom(PositionedBufferingPlaybackHandler positionedBufferingPlaybackHandler) {
		positionedBufferingPlaybackHandler.bufferingPlaybackHandler.bufferPlaybackFile().then(VoidFunc.runningCarelessly(this));

		return new PositionedPlaybackFile(positionedBufferingPlaybackHandler.positionedFile.playlistPosition, positionedBufferingPlaybackHandler.bufferingPlaybackHandler, positionedBufferingPlaybackHandler.positionedFile.file);
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
		positionedPreparingFile.positionedBufferingPlaybackHandlerPromise.then(this);

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
			currentPreparingPlaybackHandlerPromise.positionedBufferingPlaybackHandlerPromise.cancel();

		final ReentrantReadWriteLock.WriteLock writeLock = queueUpdateLock.writeLock();
		writeLock.lock();
		try {
			while (bufferingMediaPlayerPromises.size() > 0)
				bufferingMediaPlayerPromises.poll().positionedBufferingPlaybackHandlerPromise.cancel();
		} finally {
			writeLock.unlock();
		}
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
