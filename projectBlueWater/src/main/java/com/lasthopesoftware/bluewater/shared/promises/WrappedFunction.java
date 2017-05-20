package com.lasthopesoftware.bluewater.shared.promises;

import com.lasthopesoftware.promises.EmptyMessenger;

import java.util.concurrent.Callable;

public class WrappedFunction<TResult> implements Runnable {
	private final Callable<TResult> callable;
	private final EmptyMessenger<TResult> messenger;

	public WrappedFunction(Callable<TResult> callable, EmptyMessenger<TResult> messenger) {
		this.callable = callable;
		this.messenger = messenger;
	}

	@Override
	public void run() {
		try {
			messenger.sendResolution(callable.call());
		} catch (Throwable rejection) {
			messenger.sendRejection(rejection);
		}
	}
}
