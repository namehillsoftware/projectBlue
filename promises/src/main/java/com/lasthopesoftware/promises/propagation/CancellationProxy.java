package com.lasthopesoftware.promises.propagation;


import com.lasthopesoftware.promises.Promise;

public final class CancellationProxy implements Runnable {
	private final Promise<?> promise;

	public CancellationProxy(Promise<?> promise) {
		this.promise = promise;
	}

	@Override
	public void run() {
		promise.cancel();
	}
}
