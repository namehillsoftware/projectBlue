package com.lasthopesoftware.messenger.promise;


import com.lasthopesoftware.messenger.promise.propagation.PromiseProxy;
import com.vedsoft.futures.callables.CarelessOneParameterFunction;

final class PromisedResolutionResponseMessenger<Resolution, Response> extends ResolutionResponseMessenger<Resolution, Response> {
	private final CarelessOneParameterFunction<Resolution, Promise<Response>> onFulfilled;
	private final PromiseProxy<Response> promiseProxy = new PromiseProxy<>(this);

	PromisedResolutionResponseMessenger(CarelessOneParameterFunction<Resolution, Promise<Response>> onFulfilled) {
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
