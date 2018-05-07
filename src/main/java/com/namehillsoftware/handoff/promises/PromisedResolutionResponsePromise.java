package com.namehillsoftware.handoff.promises;


import com.namehillsoftware.handoff.promises.propagation.CancellationProxy;
import com.namehillsoftware.handoff.promises.response.PromisedResponse;

final class PromisedResolutionResponsePromise<Resolution, Response> extends ResolutionResponsePromise<Resolution, Response> {
	private final PromisedResponse<Resolution, Response> onFulfilled;
	private final CancellationProxy cancellationProxy = new CancellationProxy();

	PromisedResolutionResponsePromise(PromisedResponse<Resolution, Response> onFulfilled) {
		this.onFulfilled = onFulfilled;
		cancellationRequested(cancellationProxy);
	}

	@Override
	protected void respond(Resolution resolution) {
		try {
			final Promise<Response> promisedResponse = onFulfilled.promiseResponse(resolution);

			cancellationProxy.doCancel(promisedResponse);

			promisedResponse
				.then(r -> {
					resolve(r);
					return null;
				});

			promisedResponse
				.excuse(r -> {
					reject(r);
					return null;
				});

		} catch (Throwable throwable) {
			reject(throwable);
		}
	}
}
