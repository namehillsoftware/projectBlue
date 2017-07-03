package com.lasthopesoftware.messenger.promise.propagation;


import com.lasthopesoftware.messenger.Messenger;
import com.vedsoft.futures.callables.CarelessOneParameterFunction;

public final class RejectionProxy implements CarelessOneParameterFunction<Throwable, Void> {
	private final Messenger<?> reject;

	public RejectionProxy(Messenger<?> reject) {
		this.reject = reject;
	}

	@Override
	public Void resultFrom(Throwable throwable) throws Throwable {
		reject.sendRejection(throwable);
		return null;
	}
}
