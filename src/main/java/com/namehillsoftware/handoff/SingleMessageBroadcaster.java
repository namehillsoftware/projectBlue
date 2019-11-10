package com.namehillsoftware.handoff;

import com.namehillsoftware.handoff.rejections.UnhandledRejectionsReceiver;

import java.util.Collections;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicReference;

public abstract class SingleMessageBroadcaster<Resolution> extends Cancellation {

	private static final UnhandledRejectionDispatcher unhandledRejectionDispatcher = new UnhandledRejectionDispatcher();

	private final Object resolveSync = new Object();
	@SuppressWarnings("unchecked")
	private final AtomicReference<Queue<RespondingMessenger<Resolution>>> recipients = new AtomicReference<>(
			new ConcurrentLinkedQueue<>(Collections.singleton((RespondingMessenger<Resolution>)unhandledRejectionDispatcher)));

	private Message<Resolution> message;

	private static volatile UnhandledRejectionsReceiver unhandledRejectionsReceiver;

	protected static synchronized void setUnhandledRejectionsReceiver(UnhandledRejectionsReceiver receiver) {
		SingleMessageBroadcaster.unhandledRejectionsReceiver = receiver;
	}

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

	protected synchronized final void awaitResolution(RespondingMessenger<Resolution> recipient) {
		final Queue<RespondingMessenger<Resolution>> messengers = recipients
				.updateAndGet(respondingMessengers ->
					respondingMessengers.peek() == unhandledRejectionDispatcher
						? new ConcurrentLinkedQueue<>()
						: respondingMessengers);

		messengers.offer(recipient);

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
		final Queue<RespondingMessenger<Resolution>> respondingMessengers = recipients.get();
		RespondingMessenger<Resolution> r;
		while ((r = respondingMessengers.poll()) != null)
			r.respond(message);
	}

	private static class UnhandledRejectionDispatcher implements RespondingMessenger {

		@Override
		public void respond(Message message) {
			if (unhandledRejectionsReceiver != null)
				unhandledRejectionsReceiver.newUnhandledRejection(message.rejection);
		}
	}
}
