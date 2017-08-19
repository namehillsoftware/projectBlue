package com.lasthopesoftware.messenger.promises;

import com.vedsoft.futures.callables.CarelessOneParameterFunction;

final class Execution {

	static final class ExpectedResult<Resolution, Response> extends ResolutionResponseMessenger<Resolution, Response> {
		private final CarelessOneParameterFunction<Resolution, Response> onFulfilled;

		ExpectedResult(CarelessOneParameterFunction<Resolution, Response> onFulfilled) {
			this.onFulfilled = onFulfilled;
		}

		@Override
		void respond(Resolution resolution) {
			try {
				sendResolution(onFulfilled.resultFrom(resolution));
			} catch (Throwable rejection) {
				sendRejection(rejection);
			}
		}
	}

	static final class ErrorResultExecutor<TResult, TNewResult> extends RejectionResponseMessenger<TResult, TNewResult> {
		private final CarelessOneParameterFunction<Throwable, TNewResult> onFulfilled;

		ErrorResultExecutor(CarelessOneParameterFunction<Throwable, TNewResult> onFulfilled) {
			this.onFulfilled = onFulfilled;
		}

		@Override
		protected void respond(Throwable throwable) {
			try {
				sendResolution(onFulfilled.resultFrom(throwable));
			} catch (Throwable rejection) {
				sendRejection(rejection);
			}
		}
	}
}
