package com.lasthopesoftware.promises.propagation;


import com.lasthopesoftware.promises.Messenger;
import com.vedsoft.futures.callables.CarelessOneParameterFunction;

public final class ResolveWithPromiseResult<NewResult> implements CarelessOneParameterFunction<NewResult, Void> {
	private final Messenger<NewResult> resolve;

	public ResolveWithPromiseResult(Messenger<NewResult> resolve) {
		this.resolve = resolve;
	}

	@Override
	public Void resultFrom(NewResult newResult) throws Throwable {
		resolve.sendResolution(newResult);
		return null;
	}
}
