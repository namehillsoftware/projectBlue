package com.namehillsoftware.handoff;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class SingleMessageBroadcaster<Resolution> implements Messenger<Resolution> {

	private final Object resolveSync = new Object();
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
