package com.lasthopesoftware.promises.propagation;


import com.lasthopesoftware.promises.Messenger;
import com.vedsoft.futures.callables.CarelessOneParameterFunction;

public final class RejectWithPromiseError implements CarelessOneParameterFunction<Throwable, Void> {
	private final Messenger<?> reject;

	public RejectWithPromiseError(Messenger<?> reject) {
		this.reject = reject;
	}

	@Override
	public Void resultFrom(Throwable throwable) throws Throwable {
		reject.sendRejection(throwable);
		return null;
	}
}
