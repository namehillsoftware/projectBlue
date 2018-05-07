package com.namehillsoftware.handoff.promises;

import com.namehillsoftware.handoff.promises.propagation.CancellationProxy;
import com.namehillsoftware.handoff.promises.response.ImmediateResponse;
import com.namehillsoftware.handoff.promises.response.PromisedResponse;

class PromisedGuaranteedResponseMessenger<Resolution, Response> extends ResponseRoutingPromise<Resolution, Response> {

	private final CancellationProxy cancellationProxy = new CancellationProxy();
	private final PromisedResponse<Resolution, Response> onFulfilled;
	private final PromisedResponse<Throwable, Response> onRejected;
	private final InternalRejectionProxy rejectionProxy = new InternalRejectionProxy();
	private final InternalResolutionProxy resolutionProxy = new InternalResolutionProxy();

	PromisedGuaranteedResponseMessenger(PromisedResponse<Resolution, Response> onFulfilled, PromisedResponse<Throwable, Response> onRejected) {
		this.onFulfilled = onFulfilled;
		this.onRejected = onRejected;
		cancellationRequested(cancellationProxy);
	}

	@Override
	protected void respond(Resolution resolution) throws Throwable {
		proxy(onFulfilled.promiseResponse(resolution));
	}

	@Override
	protected void respond(Throwable rejection) throws Throwable {
		proxy(onRejected.promiseResponse(rejection));
	}

	private void proxy(Promise<Response> promisedResponse) {
		try {
			cancellationProxy.doCancel(promisedResponse);

			promisedResponse.then(resolutionProxy, rejectionProxy);

		} catch (Throwable throwable) {
			reject(throwable);
		}
	}

	private final class InternalResolutionProxy implements ImmediateResponse<Response, Void> {

		@Override
		public Void respond(Response resolution) {
			resolve(resolution);
			return null;
		}
	}

	private final class InternalRejectionProxy implements ImmediateResponse<Throwable, Void> {

		@Override
		public Void respond(Throwable throwable) {
			reject(throwable);
			return null;
		}
	}
}
