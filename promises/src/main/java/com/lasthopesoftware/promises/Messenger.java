package com.lasthopesoftware.promises;

import com.vedsoft.futures.runnables.OneParameterAction;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public abstract class Messenger<Input, Resolution> implements
	IResolvedPromise<Resolution>,
	IRejectedPromise,
	OneParameterAction<Runnable> {

	private final ReadWriteLock resolveSync = new ReentrantReadWriteLock();
	private final Queue<Messenger<Resolution, ?>> recipients = new ConcurrentLinkedQueue<>();
	private final Cancellation cancellation = new Cancellation();

	private boolean isResolved;
	private Resolution resolution;
	private Throwable rejection;

	protected abstract void requestResolution(Input input, Throwable throwable);

	@Override
	public final void sendRejection(Throwable error) {
		resolve(null, error);
	}

	@Override
	public final void sendResolution(Resolution resolution) {
		resolve(resolution, null);
	}

	@Override
	public final void runWith(Runnable response) {
		cancellation.runWith(response);
	}

	final void cancel() {
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

	final void awaitResolution(Messenger<Resolution, ?> recipient) {
		recipients.offer(recipient);

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

		while (recipients.size() > 0)
			recipients.poll().requestResolution(resolution, rejection);
	}
}
