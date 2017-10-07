package com.lasthopesoftware.bluewater.client.playback.queues;

import com.lasthopesoftware.bluewater.client.playback.file.PositionedFile;
import com.lasthopesoftware.bluewater.client.playback.file.PositionedPlaybackFile;
import com.lasthopesoftware.bluewater.client.playback.file.buffering.IBufferingPlaybackFile;
import com.lasthopesoftware.bluewater.client.playback.file.preparation.IPlaybackPreparer;
import com.lasthopesoftware.bluewater.client.playback.file.preparation.PreparedPlaybackFile;
import com.lasthopesoftware.messenger.promises.Promise;
import com.lasthopesoftware.messenger.promises.response.ImmediateAction;
import com.lasthopesoftware.messenger.promises.response.ImmediateResponse;
import com.lasthopesoftware.messenger.promises.response.PromisedResponse;
import com.lasthopesoftware.messenger.promises.response.ResponseAction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class PreparedPlaybackQueue
implements
	IPreparedPlaybackFileQueue,
	ResponseAction<IBufferingPlaybackFile>,
	ImmediateResponse<PositionedPreparedPlaybackFile, PositionedPlaybackFile>,
	PromisedResponse<PositionedPreparedPlaybackFile, PositionedPreparedPlaybackFile>
{
	private static final Logger logger = LoggerFactory.getLogger(PreparedPlaybackQueue.class);

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
				if (positionedPreparingFile.positionedFile.equals(newPositionedFileQueue.peek())) {
					newPositionedPreparingMediaPlayerPromises.offer(positionedPreparingFile);
					newPositionedFileQueue.poll();
					continue;
				}

				while ((positionedPreparingFile = bufferingMediaPlayerPromises.poll()) != null)
					positionedPreparingFile.preparedPlaybackFilePromise.cancel();
			}

			while ((positionedPreparingFile = newPositionedPreparingMediaPlayerPromises.poll()) != null)
				bufferingMediaPlayerPromises.offer(positionedPreparingFile);

			positionedFileQueue = newPositionedFileQueue;

			beginQueueingPreparingPlayers();

			return this;
		} finally {
			writeLock.unlock();
		}
	}

	@Override
	public Promise<PositionedPlaybackFile> promiseNextPreparedPlaybackFile(int preparedAt) {
		currentPreparingPlaybackHandlerPromise = bufferingMediaPlayerPromises.poll();
		if (currentPreparingPlaybackHandlerPromise == null) {
			currentPreparingPlaybackHandlerPromise = getNextPreparingMediaPlayerPromise(preparedAt);
			return currentPreparingPlaybackHandlerPromise != null
				? currentPreparingPlaybackHandlerPromise.promisePositionedPreparedPlaybackFile().then(this)
				: null;
		}

		return
			Promise.whenAny(
				currentPreparingPlaybackHandlerPromise.promisePositionedPreparedPlaybackFile(),
				new Promise<>(PositionedPreparedPlaybackFile.emptyHandler(currentPreparingPlaybackHandlerPromise.positionedFile)))
			.eventually(this)
			.then(this);
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

	private void beginQueueingPreparingPlayers() {
		final ReentrantReadWriteLock.WriteLock writeLock = queueUpdateLock.writeLock();
		writeLock.lock();
		try {
			if (bufferingMediaPlayerPromises.size() >= configuration.getMaxQueueSize()) return;

			final PositionedPreparingFile nextPreparingMediaPlayerPromise = getNextPreparingMediaPlayerPromise(0);
			if (nextPreparingMediaPlayerPromise == null) return;

			bufferingMediaPlayerPromises.offer(nextPreparingMediaPlayerPromise);

			nextPreparingMediaPlayerPromise.promisePositionedPreparedPlaybackFile().then(this);
		} finally {
			writeLock.unlock();
		}
	}

	@Override
	public void close() throws IOException {
		if (currentPreparingPlaybackHandlerPromise != null)
			currentPreparingPlaybackHandlerPromise.preparedPlaybackFilePromise.cancel();

		final ReentrantReadWriteLock.WriteLock writeLock = queueUpdateLock.writeLock();
		writeLock.lock();
		try {
			PositionedPreparingFile positionedPreparingFile;
			while ((positionedPreparingFile = bufferingMediaPlayerPromises.poll()) != null)
				positionedPreparingFile.preparedPlaybackFilePromise.cancel();
		} finally {
			writeLock.unlock();
		}
	}

	@Override
	public PositionedPlaybackFile respond(PositionedPreparedPlaybackFile positionedPreparedPlaybackFile) {
		positionedPreparedPlaybackFile.preparedPlaybackFile.getBufferingPlaybackFile().promiseBufferedPlaybackFile().then(ImmediateAction.perform(this));

		return new PositionedPlaybackFile(positionedPreparedPlaybackFile.preparedPlaybackFile.getPlaybackHandler(), positionedPreparedPlaybackFile.positionedFile);
	}

	@Override
	public void perform(IBufferingPlaybackFile bufferingPlaybackHandler) {
		beginQueueingPreparingPlayers();
	}

	@Override
	public Promise<PositionedPreparedPlaybackFile> promiseResponse(PositionedPreparedPlaybackFile positionedPreparedPlaybackFile) throws Throwable {
		if (!positionedPreparedPlaybackFile.isEmpty())
			return new Promise<>(positionedPreparedPlaybackFile);

		final PositionedFile positionedFile = currentPreparingPlaybackHandlerPromise.positionedFile;
		logger.warn(positionedFile + " failed to prepare in time. Cancelling and preparing again.");

		currentPreparingPlaybackHandlerPromise.preparedPlaybackFilePromise.cancel();
		currentPreparingPlaybackHandlerPromise = new PositionedPreparingFile(
			positionedFile,
			playbackPreparerTaskFactory.promisePreparedPlaybackHandler(positionedFile.getServiceFile(), 0));

		return currentPreparingPlaybackHandlerPromise.promisePositionedPreparedPlaybackFile();
	}

	private static class PositionedPreparingFile implements ImmediateResponse<PreparedPlaybackFile, PositionedPreparedPlaybackFile> {
		final PositionedFile positionedFile;
		final Promise<PreparedPlaybackFile> preparedPlaybackFilePromise;

		private PositionedPreparingFile(PositionedFile positionedFile, Promise<PreparedPlaybackFile> preparedPlaybackFilePromise) {
			this.positionedFile = positionedFile;
			this.preparedPlaybackFilePromise = preparedPlaybackFilePromise;
		}

		Promise<PositionedPreparedPlaybackFile> promisePositionedPreparedPlaybackFile() {
			return preparedPlaybackFilePromise.then(this);
		}

		@Override
		public PositionedPreparedPlaybackFile respond(PreparedPlaybackFile handler) throws Throwable {
			return new PositionedPreparedPlaybackFile(positionedFile, handler);
		}
	}
}
