package com.lasthopesoftware.promises;


import com.lasthopesoftware.promises.propagation.PromiseProxy;
import com.vedsoft.futures.callables.CarelessOneParameterFunction;

final class PromisedResolution<TResult, TNewResult> extends ResolutionMessenger<TResult, TNewResult> {
	private final CarelessOneParameterFunction<TResult, Promise<TNewResult>> onFulfilled;
	private final PromiseProxy<TNewResult> promiseProxy = new PromiseProxy<>(this);

	PromisedResolution(CarelessOneParameterFunction<TResult, Promise<TNewResult>> onFulfilled) {
		this.onFulfilled = onFulfilled;
	}

	@Override
	protected void requestResolution(TResult result) {
		try {
			promiseProxy.proxy(onFulfilled.resultFrom(result));
		} catch (Throwable throwable) {
			sendRejection(throwable);
		}
	}
}
