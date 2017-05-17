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
	private final Queue<Messenger<Resolution, ?>> resolutionRecipients = new ConcurrentLinkedQueue<>();
	private final Queue<Messenger<Throwable, ?>> rejectionRecipients = new ConcurrentLinkedQueue<>();
	private final Cancellation cancellation = new Cancellation();

	private boolean isResolved;
	private Resolution resolution;
	private Throwable rejection;

	private boolean getIsResolvedSynchronously() {
		resolveSync.readLock().lock();
		try {
			return isResolved;
		} finally {
			resolveSync.readLock().unlock();
		}
	}

	protected abstract void requestResolution(Input input);

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
		resolutionRecipients.offer(recipient);

		processQueue(resolution, rejection);
	}

	final void awaitRejection(Messenger<Throwable, ?> recipient) {
		rejectionRecipients.offer(recipient);

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
		if (!getIsResolvedSynchronously()) return;

		if (rejection == null) {
			while (resolutionRecipients.size() > 0)
				resolutionRecipients.poll().requestResolution(resolution);

			rejectionRecipients.clear();

			return;
		}

		while (rejectionRecipients.size() > 0)
			rejectionRecipients.poll().requestResolution(rejection);

		resolutionRecipients.clear();
	}
}
