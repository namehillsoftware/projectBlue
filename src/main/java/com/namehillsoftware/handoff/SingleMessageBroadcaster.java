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

	@SuppressWarnings("unchecked")
	private Queue<RespondingMessenger<Resolution>> unhandledErrorQueue = new ConcurrentLinkedQueue<>(Collections.singleton((RespondingMessenger<Resolution>)UnhandledRejectionDispatcher.instance));

	private Queue<RespondingMessenger<Resolution>> recipients = unhandledErrorQueue;

	private final Queue<RespondingMessenger<Resolution>> actualQueue = new ConcurrentLinkedQueue<>();

	private final AtomicReference<Message<Resolution>> atomicMessage = new AtomicReference<>();

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
		if (recipients == unhandledErrorQueue)
			recipients = actualQueue;

		unhandledErrorQueue = null;

		recipients.offer(recipient);

		if (isResolvedSynchronously())
			dispatchMessage(atomicMessage.get());
	}

	private boolean isResolvedSynchronously() {
		return atomicMessage.get() != null;
	}

	private void resolve(Resolution resolution, Throwable rejection) {
		if (atomicMessage.get() == null) {
			while (!atomicMessage.compareAndSet(null, new Message<>(resolution, rejection)));
		}

		dispatchMessage(atomicMessage.get());
	}

	private void dispatchMessage(Message<Resolution> message) {
		RespondingMessenger<Resolution> r;
		while ((r = recipients.poll()) != null)
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
