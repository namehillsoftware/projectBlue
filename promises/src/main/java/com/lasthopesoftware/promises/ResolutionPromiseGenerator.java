package com.lasthopesoftware.promises;


import com.lasthopesoftware.promises.propagation.PromiseProxy;
import com.vedsoft.futures.callables.CarelessOneParameterFunction;

final class ResolutionPromiseGenerator<Resolution, Response> extends ResolutionRespondingPromise<Resolution, Response> {
	private final CarelessOneParameterFunction<Resolution, Promise<Response>> onFulfilled;
	private final PromiseProxy<Response> promiseProxy = new PromiseProxy<>(this);

	ResolutionPromiseGenerator(CarelessOneParameterFunction<Resolution, Promise<Response>> onFulfilled) {
		this.onFulfilled = onFulfilled;
	}

	@Override
	void respond(Resolution resolution) {
		try {
			promiseProxy.proxy(onFulfilled.resultFrom(resolution));
		} catch (Throwable throwable) {
			sendRejection(throwable);
		}
	}
}
