package com.namehillsoftware.handoff;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public abstract class SingleMessageBroadcaster<Resolution> extends Cancellation {

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

	protected final void awaitResolution(RespondingMessenger<Resolution> recipient) {
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

	private synchronized void dispatchMessage(Message<Resolution> message) {
		RespondingMessenger<Resolution> r = recipients.poll();
		if (r != null) {
			do {
				r.respond(message);
			} while ((r = recipients.poll()) != null);

			return;
		}

		if (message.rejection == null) return;

		final Rejections.ReceiveUnhandledRejections unhandledRejections = Rejections.getUnhandledRejectionsHandler();
		if (unhandledRejections != null)
			unhandledRejections.newUnhandledRejection(message.rejection);
	}
}
