package com.namehillsoftware.handoff.promises.propagation;


import com.namehillsoftware.handoff.promises.Promise;
import com.namehillsoftware.handoff.promises.queued.cancellation.CancellationToken;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public final class CancellationProxy extends CancellationToken {
	private final Queue<Promise<?>> cancellablePromises = new ConcurrentLinkedQueue<Promise<?>>();

	public Runnable doCancel(Promise<?> promise) {
		cancellablePromises.offer(promise);

		if (isCancelled()) run();

		return this;
	}

	@Override
	public synchronized void run() {
		super.run();

		Promise<?> cancellingPromise;
		while ((cancellingPromise = cancellablePromises.poll()) != null)
			cancellingPromise.cancel();
	}
}
