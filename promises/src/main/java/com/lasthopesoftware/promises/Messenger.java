package com.lasthopesoftware.promises;

import com.vedsoft.futures.runnables.OneParameterAction;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Created by david on 3/31/17.
 */
abstract class Messenger<Input, Resolution> implements
	IResolvedPromise<Resolution>,
	IRejectedPromise,
	OneParameterAction<Runnable> {

	private final ReadWriteLock resolveSync = new ReentrantReadWriteLock();

	private final Queue<Messenger<Resolution, ?>> responses = new ConcurrentLinkedQueue<>();

	private boolean isResolved;
	private Resolution resolution;
	private Throwable rejection;
	private final Cancellation cancellation = new Cancellation();

	public abstract void message(Input input, Throwable throwable);

	@Override
	public void withError(Throwable error) {
		resolve(null, error);
	}

	@Override
	public void withResult(Resolution resolution) {
		resolve(resolution, null);
	}

	@Override
	public void runWith(Runnable response) {
		cancellation.runWith(response);
	}

	void cancel() {
		final boolean isResolvedLocally;
		resolveSync.readLock().lock();
		try {
			isResolvedLocally = isResolved;
		} finally {
			resolveSync.readLock().unlock();
		}

		if (!isResolvedLocally)
			cancellation.cancel();
	}

	void awaitResolution(Messenger<Resolution, ?> response) {
		responses.offer(response);

		processQueue(resolution, rejection);
	}

	private void resolve(Resolution resolution, Throwable rejection) {
		resolveSync.writeLock().lock();
		try {
			if (isResolved) return;

			this.resolution = resolution;
			this.rejection = rejection;

			isResolved = true;
		} finally {
			resolveSync.writeLock().unlock();
		}

		processQueue(resolution, rejection);
	}

	private void processQueue(Resolution resolution, Throwable rejection) {
		resolveSync.readLock().lock();
		try {
			if (!isResolved) return;
		} finally {
			resolveSync.readLock().unlock();
		}

		while (responses.size() > 0)
			responses.poll().message(resolution, rejection);
	}
}
