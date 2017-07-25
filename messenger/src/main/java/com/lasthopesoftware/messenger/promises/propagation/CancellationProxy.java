package com.lasthopesoftware.messenger.promises.propagation;


import com.lasthopesoftware.messenger.promises.Promise;
import com.lasthopesoftware.messenger.promises.queued.cancellation.CancellationToken;

import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

public final class CancellationProxy implements Runnable {
	private final Queue<Promise<?>> cancellablePromises = new LinkedBlockingQueue<>();
	private final CancellationToken cancellationToken = new CancellationToken();

	public Runnable doCancel(Promise<?> promise) {
		cancellablePromises.offer(promise);

		if (cancellationToken.isCancelled()) run();

		return this;
	}

	@Override
	public void run() {
		cancellationToken.run();

		Promise<?> cancellingPromise;
		while ((cancellingPromise = cancellablePromises.poll()) != null)
			cancellingPromise.cancel();
	}
}
