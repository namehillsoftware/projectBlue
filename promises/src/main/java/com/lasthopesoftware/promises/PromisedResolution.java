package com.lasthopesoftware.promises;


import com.vedsoft.futures.callables.CarelessOneParameterFunction;

final class PromisedResolution<TResult, TNewResult> extends ResolutionMessenger<TResult, TNewResult> {
	private final CarelessOneParameterFunction<TResult, Promise<TNewResult>> onFulfilled;

	PromisedResolution(CarelessOneParameterFunction<TResult, Promise<TNewResult>> onFulfilled) {
		this.onFulfilled = onFulfilled;
	}

	@Override
	protected void requestResolution(TResult result) {
		try {
			final Promise<TNewResult> fulfilledPromise = onFulfilled.resultFrom(result);

			runWith(new PromisedCancellable<>(fulfilledPromise));

			fulfilledPromise
				.next(new ResolveWithPromiseResult<>(this))
				.error(new RejectWithPromiseError<>(this));
		} catch (Throwable rejection) {
			sendRejection(rejection);
		}
	}

	private static class PromisedCancellable<TNewResult> implements Runnable {
		private final Promise<TNewResult> fulfilledPromise;

		PromisedCancellable(Promise<TNewResult> fulfilledPromise) {
			this.fulfilledPromise = fulfilledPromise;
		}

		@Override
		public void run() {
			fulfilledPromise.cancel();
		}
	}

	private static final class ResolveWithPromiseResult<NewResult> extends ResolutionMessenger<NewResult, Void> {
		private final IResolvedPromise<NewResult> resolve;

		ResolveWithPromiseResult(IResolvedPromise<NewResult> resolve) {
			this.resolve = resolve;
		}

		@Override
		protected void requestResolution(NewResult newResult) {
			resolve.sendResolution(newResult);
			sendResolution(null);
		}
	}

	private static final class RejectWithPromiseError<NewResult> extends ErrorMessenger<NewResult, Void> {
		private final IRejectedPromise reject;

		RejectWithPromiseError(IRejectedPromise reject) {
			this.reject = reject;
		}

		@Override
		protected void requestResolution(Throwable throwable) {
			reject.sendRejection(throwable);
			sendResolution(null);
		}
	}
}
