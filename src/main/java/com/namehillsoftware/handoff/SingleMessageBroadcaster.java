package com.namehillsoftware.handoff;

import com.namehillsoftware.handoff.rejections.UnhandledRejectionsReceiver;

import java.util.Collections;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicReference;

public abstract class SingleMessageBroadcaster<Resolution> extends Cancellation {

	private static volatile UnhandledRejectionsReceiver unhandledRejectionsReceiver;

	protected static synchronized void setUnhandledRejectionsReceiver(UnhandledRejectionsReceiver receiver) {
		SingleMessageBroadcaster.unhandledRejectionsReceiver = receiver;
	}

	private final Object resolveSync = new Object();

	@SuppressWarnings("unchecked")
	private final Queue<RespondingMessenger<Resolution>> unhandledErrorQueue = new ConcurrentLinkedQueue<>(Collections.singleton((RespondingMessenger<Resolution>)UnhandledRejectionDispatcher.instance));

	private final Queue<RespondingMessenger<Resolution>> actualQueue = new ConcurrentLinkedQueue<>();

	private final AtomicReference<Queue<RespondingMessenger<Resolution>>> recipients = new AtomicReference<>(unhandledErrorQueue);

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
		recipients.updateAndGet(respondingMessengers ->
			respondingMessengers == unhandledErrorQueue
				? actualQueue
				: respondingMessengers)
			.offer(recipient);

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
		while ((r = recipients.get().poll()) != null)
			r.respond(message);
	}

	private static final class UnhandledRejectionDispatcher implements RespondingMessenger {

		private static final UnhandledRejectionDispatcher instance = new UnhandledRejectionDispatcher();

		@Override
		public void respond(Message message) {
			if (unhandledRejectionsReceiver != null)
				unhandledRejectionsReceiver.newUnhandledRejection(message.rejection);
		}
	}
}
