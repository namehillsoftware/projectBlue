package com.lasthopesoftware.messenger;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class SingleMessageBroadcaster<Resolution> implements Messenger<Resolution> {

	private final ReadWriteLock resolveSync = new ReentrantReadWriteLock();
	private final Queue<RespondingMessenger<Resolution>> recipients = new ConcurrentLinkedQueue<>();
	private final Cancellation cancellation = new Cancellation();

	private Message<Resolution> message;

	@Override
	public final void sendRejection(Throwable error) {
		resolve(null, error);
	}

	@Override
	public final void sendResolution(Resolution resolution) {
		resolve(resolution, null);
	}

	@Override
	public final void cancellationRequested(Runnable reaction) {
		cancellation.respondToCancellation(reaction);
	}

	public final void cancel() {
		if (!isResolvedSynchronously())
			cancellation.cancel();
	}

	public final void awaitResolution(RespondingMessenger<Resolution> recipient) {
		recipients.offer(recipient);

		if (isResolvedSynchronously())
			dispatchMessage(message);
	}

	private boolean isResolvedSynchronously() {
		final Lock readLock = resolveSync.readLock();
		readLock.lock();
		try {
			return message != null;
		} finally {
			readLock.unlock();
		}
	}

	private void resolve(Resolution resolution, Throwable rejection) {
		resolveSync.writeLock().lock();
		try {
			if (message != null) return;

			message = new Message<>(resolution, rejection);
		} finally {
			resolveSync.writeLock().unlock();
		}

		dispatchMessage(message);
	}

	private synchronized void dispatchMessage(Message<Resolution> message) {
		RespondingMessenger<Resolution> r;
		while ((r = recipients.poll()) != null)
			r.respond(message);
	}
}
