package com.namehillsoftware.handoff;

import com.namehillsoftware.handoff.errors.ReceiveUnhandledRejections;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public abstract class SingleMessageBroadcaster<Resolution> extends Cancellation {

	private static final Object uncaughtHandlerSynchronization = new Object();
	private static volatile ReceiveUnhandledRejections unhandledRejections;

	public static void setUnhandledRejectionsReceiver(ReceiveUnhandledRejections unhandledRejections) {
		synchronized (uncaughtHandlerSynchronization) {
			SingleMessageBroadcaster.unhandledRejections = unhandledRejections;
		}
	}

	private final Object resolveSync = new Object();
	private final Queue<RespondingMessenger<Resolution>> recipients = new ConcurrentLinkedQueue<>();

	private Message<Resolution> message;

	protected final void reject(Throwable error) {
		resolve(null, error);
	}

	protected final void resolve(Resolution resolution) {
		resolve(resolution, null);
	}

	public final void cancel() {
		if (!isResolvedSynchronously())
			super.cancel();
	}

	public final void awaitResolution(RespondingMessenger<Resolution> recipient) {
		recipients.offer(recipient);

		if (isResolvedSynchronously())
			dispatchMessage(message);
	}

	private boolean isResolvedSynchronously() {
		synchronized (resolveSync) {
			return message != null;
		}
	}

	private void resolve(Resolution resolution, Throwable rejection) {
		synchronized (resolveSync) {
			if (message != null) return;

			message = new Message<>(resolution, rejection);
		}

		dispatchMessage(message);
	}

	private void dispatchMessage(Message<Resolution> message) {
		RespondingMessenger<Resolution> r;
		while ((r = recipients.poll()) != null)
			r.respond(message);
	}
}
