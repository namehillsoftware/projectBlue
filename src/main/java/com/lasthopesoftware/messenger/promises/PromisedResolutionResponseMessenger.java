package com.lasthopesoftware.messenger.promises;


import com.lasthopesoftware.messenger.promises.propagation.PromiseProxy;
import com.lasthopesoftware.messenger.promises.response.PromisedResponse;

final class PromisedResolutionResponseMessenger<Resolution, Response> extends ResolutionResponseMessenger<Resolution, Response> {
	private final PromisedResponse<Resolution, Response> onFulfilled;
	private final PromiseProxy<Response> promiseProxy = new PromiseProxy<Response>(this);

	PromisedResolutionResponseMessenger(PromisedResponse<Resolution, Response> onFulfilled) {
		this.onFulfilled = onFulfilled;
	}

	@Override
	void respond(Resolution resolution) {
		try {
			promiseProxy.proxy(onFulfilled.promiseResponse(resolution));
		} catch (Throwable throwable) {
			sendRejection(throwable);
		}
	}
}
