package com.lasthopesoftware.bluewater.shared.promises;

import com.lasthopesoftware.promises.EmptyMessenger;
import com.lasthopesoftware.promises.Messenger;
import com.vedsoft.futures.callables.CarelessFunction;

import java.util.concurrent.Callable;

public class WrappedFunction<TResult> implements Runnable {
	private final CarelessFunction<TResult> callable;
	private final Messenger<TResult> messenger;

	public WrappedFunction(CarelessFunction<TResult> callable, Messenger<TResult> messenger) {
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
