package com.lasthopesoftware.messenger.promises;

import com.lasthopesoftware.messenger.promises.response.ImmediateResponse;

final class Execution {

	static final class ExpectedResult<Resolution, Response> extends ResolutionResponseMessenger<Resolution, Response> {
		private final ImmediateResponse<Resolution, Response> onFulfilled;

		ExpectedResult(ImmediateResponse<Resolution, Response> onFulfilled) {
			this.onFulfilled = onFulfilled;
		}

		@Override
		void respond(Resolution resolution) {
			try {
				sendResolution(onFulfilled.respond(resolution));
			} catch (Throwable rejection) {
				sendRejection(rejection);
			}
		}
	}

	static final class ErrorResultExecutor<TResult, TNewResult> extends RejectionResponseMessenger<TResult, TNewResult> {
		private final ImmediateResponse<Throwable, TNewResult> onFulfilled;

		ErrorResultExecutor(ImmediateResponse<Throwable, TNewResult> onFulfilled) {
			this.onFulfilled = onFulfilled;
		}

		@Override
		protected void respond(Throwable throwable) {
			try {
				sendResolution(onFulfilled.respond(throwable));
			} catch (Throwable rejection) {
				sendRejection(rejection);
			}
		}
	}
}
