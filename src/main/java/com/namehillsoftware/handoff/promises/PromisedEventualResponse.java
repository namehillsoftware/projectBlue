package com.namehillsoftware.handoff.promises;

import com.namehillsoftware.handoff.promises.propagation.CancellationProxy;
import com.namehillsoftware.handoff.promises.response.ImmediateResponse;
import com.namehillsoftware.handoff.promises.response.PromisedResponse;

class PromisedEventualResponse<Resolution, Response> extends PromiseResponse<Resolution, Response> {

	private final CancellationProxy cancellationProxy = new CancellationProxy();
	private final PromisedResponse<Resolution, Response> onFulfilled;
	private final PromisedResponse<Throwable, Response> onRejected;
	private final InternalRejectionProxy rejectionProxy = new InternalRejectionProxy();
	private final InternalResolutionProxy resolutionProxy = new InternalResolutionProxy();

	PromisedEventualResponse(PromisedResponse<Resolution, Response> onFulfilled) {
		this(onFulfilled, null);
	}

	PromisedEventualResponse(PromisedResponse<Resolution, Response> onFulfilled, PromisedResponse<Throwable, Response> onRejected) {
		this.onFulfilled = onFulfilled;
		this.onRejected = onRejected;
		respondToCancellation(cancellationProxy);
	}

	@Override
	protected void respond(Resolution resolution) throws Throwable {
		proxy(onFulfilled.promiseResponse(resolution));
	}

	@Override
	protected void respond(Throwable reason) throws Throwable {
		if (onRejected != null)
			proxy(onRejected.promiseResponse(reason));
		else
			reject(reason);
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
