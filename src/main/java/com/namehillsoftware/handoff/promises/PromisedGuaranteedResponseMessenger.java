package com.namehillsoftware.handoff.promises;

import com.namehillsoftware.handoff.promises.propagation.PromiseProxy;
import com.namehillsoftware.handoff.promises.response.PromisedResponse;

class PromisedGuaranteedResponseMessenger<Resolution, Response> extends ResponseRoutingPromise<Resolution, Response> {

	private final PromiseProxy<Response> promiseProxy = new PromiseProxy<>(this);
	private final PromisedResponse<Resolution, Response> onFulfilled;
	private final PromisedResponse<Throwable, Response> onRejected;

	PromisedGuaranteedResponseMessenger(PromisedResponse<Resolution, Response> onFulfilled, PromisedResponse<Throwable, Response> onRejected) {
		this.onFulfilled = onFulfilled;
		this.onRejected = onRejected;
	}

	@Override
	protected void respond(Resolution resolution) throws Throwable {
		promiseProxy.proxy(onFulfilled.promiseResponse(resolution));
	}

	@Override
	protected void respond(Throwable rejection) throws Throwable {
		promiseProxy.proxy(onRejected.promiseResponse(rejection));
	}
}
