package com.lasthopesoftware.promises;

import com.vedsoft.futures.runnables.OneParameterAction;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

abstract class AwaitingMessenger<Input, Resolution> implements Messenger<Resolution>, OneParameterAction<Runnable> {

	private final ReadWriteLock resolveSync = new ReentrantReadWriteLock();
	private final Queue<AwaitingMessenger<Resolution, ?>> recipients = new ConcurrentLinkedQueue<>();
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
		cancellationRequested(response);
	}

	@Override
	public final void cancellationRequested(Runnable reaction) {
		cancellation.runWith(reaction);
	}

	final void cancel() {
		if (!isResolvedSynchronously())
			cancellation.cancel();
	}

	final void awaitResolution(AwaitingMessenger<Resolution, ?> recipient) {
		recipients.offer(recipient);

		if (isResolvedSynchronously())
			dispatchMessage(resolution, rejection);
	}

	private boolean isResolvedSynchronously() {
		final Lock readLock = resolveSync.readLock();
		readLock.lock();
		try {
			return isResolved;
		} finally {
			readLock.unlock();
		}
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

		dispatchMessage(resolution, rejection);
	}

	private synchronized void dispatchMessage(Resolution resolution, Throwable rejection) {
		AwaitingMessenger<Resolution, ?> r;
		while ((r = recipients.poll()) != null)
			r.requestResolution(resolution, rejection);
	}
}
