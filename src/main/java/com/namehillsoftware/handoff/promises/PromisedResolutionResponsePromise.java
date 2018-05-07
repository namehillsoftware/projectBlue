package com.namehillsoftware.handoff.promises;


import com.namehillsoftware.handoff.promises.propagation.CancellationProxy;
import com.namehillsoftware.handoff.promises.response.ImmediateResponse;
import com.namehillsoftware.handoff.promises.response.PromisedResponse;

final class PromisedResolutionResponsePromise<Resolution, Response> extends ResolutionResponsePromise<Resolution, Response> {
	private final PromisedResponse<Resolution, Response> onFulfilled;
	private final CancellationProxy cancellationProxy = new CancellationProxy();
	private final ImmediateResponse<Throwable, Void> rejectionProxy = new InternalRejectionProxy();
	private final InternalResolutionProxy resolutionProxy = new InternalResolutionProxy();

	PromisedResolutionResponsePromise(PromisedResponse<Resolution, Response> onFulfilled) {
		this.onFulfilled = onFulfilled;
		cancellationRequested(cancellationProxy);
	}

	@Override
	protected void respond(Resolution resolution) {
		try {
			final Promise<Response> promisedResponse = onFulfilled.promiseResponse(resolution);

			cancellationProxy.doCancel(promisedResponse);

			promisedResponse.then(resolutionProxy, rejectionProxy);

		} catch (Throwable throwable) {
			reject(throwable);
		}
	}

	private class InternalResolutionProxy implements ImmediateResponse<Response, Void> {

		@Override
		public Void respond(Response resolution) {
			resolve(resolution);
			return null;
		}
	}

	private class InternalRejectionProxy implements ImmediateResponse<Throwable, Void> {

		@Override
		public Void respond(Throwable throwable) {
			reject(throwable);
			return null;
		}
	}
}
