package com.lasthopesoftware.bluewater.client.playback.queues;

import com.lasthopesoftware.bluewater.client.playback.file.PositionedFile;
import com.lasthopesoftware.bluewater.client.playback.file.PositionedPlaybackFile;
import com.lasthopesoftware.bluewater.client.playback.file.buffering.IBufferingPlaybackHandler;
import com.lasthopesoftware.bluewater.client.playback.file.preparation.IPlaybackPreparer;
import com.lasthopesoftware.messenger.promises.Promise;
import com.lasthopesoftware.messenger.promises.response.ImmediateAction;
import com.lasthopesoftware.messenger.promises.response.ImmediateResponse;
import com.lasthopesoftware.messenger.promises.response.ResponseAction;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class PreparedPlaybackQueue implements
	IPreparedPlaybackFileQueue,
	ResponseAction<IBufferingPlaybackHandler>,
	ImmediateResponse<PositionedBufferingPlaybackHandler, PositionedPlaybackFile>
{
	private final ReentrantReadWriteLock queueUpdateLock = new ReentrantReadWriteLock();

	private final IPreparedPlaybackQueueConfiguration configuration;
	private final IPlaybackPreparer playbackPreparerTaskFactory;
	private final Queue<PositionedPreparingFile> bufferingMediaPlayerPromises;

	private IPositionedFileQueue positionedFileQueue;

	private PositionedPreparingFile currentPreparingPlaybackHandlerPromise;

	public PreparedPlaybackQueue(IPreparedPlaybackQueueConfiguration configuration, IPlaybackPreparer playbackPreparerTaskFactory, IPositionedFileQueue positionedFileQueue) {
		this.configuration = configuration;
		this.playbackPreparerTaskFactory = playbackPreparerTaskFactory;
		this.positionedFileQueue = positionedFileQueue;
		bufferingMediaPlayerPromises = new ArrayDeque<>(configuration.getMaxQueueSize());
	}

	public PreparedPlaybackQueue updateQueue(IPositionedFileQueue newPositionedFileQueue) {
		final ReentrantReadWriteLock.WriteLock writeLock = queueUpdateLock.writeLock();
		writeLock.lock();
		try {
			final Queue<PositionedPreparingFile> newPositionedPreparingMediaPlayerPromises = new ArrayDeque<>(configuration.getMaxQueueSize());

			PositionedPreparingFile positionedPreparingFile;
			while ((positionedPreparingFile = bufferingMediaPlayerPromises.poll()) != null) {
				final PositionedFile positionedFile = newPositionedFileQueue.peek();

				if (positionedPreparingFile.positionedFile.equals(positionedFile)) {
					newPositionedPreparingMediaPlayerPromises.offer(positionedPreparingFile);
					newPositionedFileQueue.poll();
					continue;
				}

				while ((positionedPreparingFile = bufferingMediaPlayerPromises.poll()) != null) {
					positionedPreparingFile.bufferingPlaybackHandlerPromise.cancel();
				}
			}

			while ((positionedPreparingFile = newPositionedPreparingMediaPlayerPromises.poll()) != null)
				bufferingMediaPlayerPromises.offer(positionedPreparingFile);

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
				playbackPreparerTaskFactory.promisePreparedPlaybackHandler(positionedFile.getServiceFile(), preparedAt));
	}

	@Override
	public PositionedPlaybackFile respond(PositionedBufferingPlaybackHandler positionedBufferingPlaybackHandler) {
		positionedBufferingPlaybackHandler.bufferingPlaybackHandler.bufferPlaybackFile().then(ImmediateAction.perform(this));

		return new PositionedPlaybackFile(positionedBufferingPlaybackHandler.bufferingPlaybackHandler, positionedBufferingPlaybackHandler.positionedFile);
	}

	@Override
	public void perform(IBufferingPlaybackHandler bufferingPlaybackHandler) {
		final ReentrantReadWriteLock.ReadLock readLock = queueUpdateLock.readLock();
		readLock.lock();
		try {
			if (bufferingMediaPlayerPromises.size() >= configuration.getMaxQueueSize()) return;
		} finally {
			readLock.unlock();
		}

		final PositionedPreparingFile nextPreparingMediaPlayerPromise = getNextPreparingMediaPlayerPromise(0);
		if (nextPreparingMediaPlayerPromise != null)
			enqueuePositionedPreparingFile(nextPreparingMediaPlayerPromise);
	}

	private void enqueuePositionedPreparingFile(PositionedPreparingFile positionedPreparingFile) {
		final ReentrantReadWriteLock.WriteLock writeLock = queueUpdateLock.writeLock();
		writeLock.lock();
		try {
			bufferingMediaPlayerPromises.offer(positionedPreparingFile);
		} finally {
			writeLock.unlock();
		}

		positionedPreparingFile.promisePositionedBufferingPlaybackHandler().then(this);
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
