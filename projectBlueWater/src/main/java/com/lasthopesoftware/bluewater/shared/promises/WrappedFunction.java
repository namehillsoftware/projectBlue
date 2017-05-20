package com.lasthopesoftware.bluewater.shared.promises;

import com.lasthopesoftware.promises.EmptyMessenger;
import com.vedsoft.futures.callables.CarelessFunction;

public class WrappedFunction<TResult> implements Runnable {
	private final CarelessFunction<TResult> callable;
	private final EmptyMessenger<TResult> messenger;

	public WrappedFunction(CarelessFunction<TResult> callable, EmptyMessenger<TResult> messenger) {
		this.callable = callable;
		this.messenger = messenger;
	}

	@Override
	public void run() {
		try {
			messenger.sendResolution(this.callable.result());
		} catch (Throwable rejection) {
			messenger.sendRejection(rejection);
		}
	}
}
