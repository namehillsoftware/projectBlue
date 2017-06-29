package com.lasthopesoftware.promises;


import com.lasthopesoftware.promises.propagation.CancellationProxy;
import com.vedsoft.futures.callables.CarelessOneParameterFunction;

final class PromisedResolutionResponsePromise<Resolution, Response> extends ResolutionResponsePromise<Resolution, Response> {
	private final CarelessOneParameterFunction<Resolution, Promise<Response>> onFulfilled;
	private final CancellationProxy cancellationProxy = new CancellationProxy();

	PromisedResolutionResponsePromise(CarelessOneParameterFunction<Resolution, Promise<Response>> onFulfilled) {
		this.onFulfilled = onFulfilled;
		cancellationRequested(cancellationProxy);
	}

	@Override
	void respond(Resolution resolution) {
		try {
			final Promise<Response> promisedResponse = onFulfilled.resultFrom(resolution);
			cancellationProxy.doCancel(promisedResponse);

			promisedResponse.next(new ResponseTunnel<>(this));
			promisedResponse.error(new RejectionTunnel(this));
		} catch (Throwable throwable) {
			sendRejection(throwable);
		}
	}

	private static final class ResponseTunnel<Resolution, Response> implements CarelessOneParameterFunction<Response, Object> {

		private final PromisedResolutionResponsePromise<Resolution, Response> promiseGenerator;

		ResponseTunnel(PromisedResolutionResponsePromise<Resolution, Response> promiseGenerator) {
			this.promiseGenerator = promiseGenerator;
		}

		@Override
		public Object resultFrom(Response response) throws Throwable {
			promiseGenerator.sendResolution(response);
			return null;
		}
	}

	private static final class RejectionTunnel implements CarelessOneParameterFunction<Throwable, Object> {

		final PromisedResolutionResponsePromise promisedResolutionResponsePromise;

		private RejectionTunnel(PromisedResolutionResponsePromise promisedResolutionResponsePromise) {
			this.promisedResolutionResponsePromise = promisedResolutionResponsePromise;
		}

		@Override
		public Object resultFrom(Throwable error) throws Throwable {
			promisedResolutionResponsePromise.sendRejection(error);
			return null;
		}
	}
}
