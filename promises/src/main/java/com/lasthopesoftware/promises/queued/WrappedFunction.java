package com.lasthopesoftware.promises.queued;

import com.lasthopesoftware.promises.Messenger;
import com.vedsoft.futures.callables.CarelessFunction;

class WrappedFunction<TResult> implements Runnable {
	private final CarelessFunction<TResult> callable;
	private final Messenger<TResult> messenger;

	WrappedFunction(CarelessFunction<TResult> callable, Messenger<TResult> messenger) {
		this.callable = callable;
		this.messenger = messenger;
	}

	@Override
	public void run() {
		try {
			messenger.sendResolution(callable.result());
		} catch (Throwable rejection) {
			messenger.sendRejection(rejection);
		}
	}
}
