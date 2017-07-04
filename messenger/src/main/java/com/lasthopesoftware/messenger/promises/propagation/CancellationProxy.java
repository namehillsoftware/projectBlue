package com.lasthopesoftware.messenger.promises.propagation;


import com.lasthopesoftware.messenger.promises.Promise;

import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

public final class CancellationProxy implements Runnable {
	private final Queue<Promise<?>> cancellablePromises = new LinkedBlockingQueue<>();
	private volatile boolean isCancelled;

	public void doCancel(Promise<?> promise){
		cancellablePromises.offer(promise);

		if (isCancelled) run();
	}

	@Override
	public synchronized void run() {
		isCancelled = true;

		Promise<?> cancellingPromise;
		while ((cancellingPromise = cancellablePromises.poll()) != null)
			cancellingPromise.cancel();
	}
}
