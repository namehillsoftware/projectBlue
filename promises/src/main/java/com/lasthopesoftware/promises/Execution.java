package com.lasthopesoftware.promises;

import com.vedsoft.futures.callables.CarelessOneParameterFunction;

final class Execution {

	static final class MessengerTunnel<Result> implements Messenger<Result> {

		private final AwaitingMessenger<Result> messenger;

		MessengerTunnel(AwaitingMessenger<Result> messenger) {
			this.messenger = messenger;
		}

		@Override
		public void sendResolution(Result result) {
			messenger.sendResolution(result);
		}

		@Override
		public void sendRejection(Throwable error) {
			messenger.sendRejection(error);
		}

		@Override
		public void cancellationRequested(Runnable response) {
			messenger.cancellationRequested(response);
		}
	}

	/**
	 * Created by david on 10/8/16.
	 */
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
