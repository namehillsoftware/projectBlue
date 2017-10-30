package com.lasthopesoftware.messenger.promises.propagation;


import com.lasthopesoftware.messenger.promises.Promise;
import com.lasthopesoftware.messenger.promises.queued.cancellation.CancellationToken;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;

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
