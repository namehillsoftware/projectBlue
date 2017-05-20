package com.lasthopesoftware.promises;

import com.vedsoft.futures.callables.CarelessOneParameterFunction;
import com.vedsoft.futures.callables.CarelessTwoParameterFunction;
import com.vedsoft.futures.runnables.FourParameterAction;
import com.vedsoft.futures.runnables.OneParameterAction;
import com.vedsoft.futures.runnables.ThreeParameterAction;
import com.vedsoft.futures.runnables.TwoParameterAction;

import java.util.concurrent.Callable;

final class Execution {

	static final class InternalCancellablePromiseExecutor<Result> extends EmptyMessenger<Result> {
		private final ThreeParameterAction<IResolvedPromise<Result>, IRejectedPromise, OneParameterAction<Runnable>> executor;

		InternalCancellablePromiseExecutor(ThreeParameterAction<IResolvedPromise<Result>, IRejectedPromise, OneParameterAction<Runnable>> executor) {
			this.executor = executor;
		}

		@Override
		public void requestResolution() {
			executor.runWith(this, this, this);
		}
	}

	/**
	 * Created by david on 10/8/16.
	 */
	static final class InternalPromiseExecutor<Result> extends EmptyMessenger<Result> {
		private final TwoParameterAction<IResolvedPromise<Result>, IRejectedPromise> executor;

		InternalPromiseExecutor(TwoParameterAction<IResolvedPromise<Result>, IRejectedPromise> executor) {
			this.executor = executor;
		}

		@Override
		public void requestResolution() {
			executor.runWith(this, this);
		}
	}

	static final class InternalExpectedPromiseExecutor<Result> extends EmptyMessenger<Result> {
		private final Callable<Result> executor;

		InternalExpectedPromiseExecutor(Callable<Result> executor) {
			this.executor = executor;
		}

		@Override
		public void requestResolution() {
			try {
				sendResolution(executor.call());
			} catch (Throwable rejection) {
				sendRejection(rejection);
			}
		}
	}

	static final class PassThroughCallable<PassThroughResult> extends EmptyMessenger<PassThroughResult> {
		private final PassThroughResult passThroughResult;

		PassThroughCallable(PassThroughResult passThroughResult) {
			this.passThroughResult = passThroughResult;
		}

		@Override
		public void requestResolution() {
			sendResolution(passThroughResult);
		}
	}

	static final class Cancellable {

		/**
		 * Created by david on 10/30/16.
		 */
		static final class ExpectedResultCancellableExecutor<Result, NewResult> extends ResolutionMessenger<Result, NewResult> {
			private final CarelessTwoParameterFunction<Result, OneParameterAction<Runnable>, NewResult> onFulfilled;

			ExpectedResultCancellableExecutor(CarelessTwoParameterFunction<Result, OneParameterAction<Runnable>, NewResult> onFulfilled) {
				this.onFulfilled = onFulfilled;
			}

			@Override
			protected void requestResolution(Result result) {
				try {
					sendResolution(onFulfilled.resultFrom(result, this));
				} catch (Throwable rejection) {
					sendRejection(rejection);
				}
			}
		}

		/**
		 * Created by david on 10/30/16.
		 */
		static final class ErrorPropagatingCancellableExecutor<TResult, TNewResult> extends ResolutionMessenger<TResult, TNewResult> {
			private final FourParameterAction<TResult, IResolvedPromise<TNewResult>, IRejectedPromise, OneParameterAction<Runnable>> onFulfilled;

			ErrorPropagatingCancellableExecutor(FourParameterAction<TResult, IResolvedPromise<TNewResult>, IRejectedPromise, OneParameterAction<Runnable>> onFulfilled) {
				this.onFulfilled = onFulfilled;
			}

			@Override
			protected void requestResolution(TResult result) {
				onFulfilled.runWith(result, this, this, this);
			}
		}

		/**
		 * Created by david on 10/30/16.
		 */
		static final class RejectionDependentCancellableExecutor<TResult, TNewRejectedResult> extends ErrorMessenger<TResult, TNewRejectedResult> {
			private final FourParameterAction<Throwable, IResolvedPromise<TNewRejectedResult>, IRejectedPromise, OneParameterAction<Runnable>> onRejected;

			RejectionDependentCancellableExecutor(FourParameterAction<Throwable, IResolvedPromise<TNewRejectedResult>, IRejectedPromise, OneParameterAction<Runnable>> onRejected) {
				this.onRejected = onRejected;
			}

			@Override
			protected void requestResolution(Throwable throwable) {
				onRejected.runWith(throwable, this, this, this);
			}
		}

		static final class RejectionDependentCancellableCaller<Result, NewResult> extends ErrorMessenger<Result, NewResult> {
			private final CarelessTwoParameterFunction<Throwable, OneParameterAction<Runnable>, NewResult> onFulfilled;

			RejectionDependentCancellableCaller(CarelessTwoParameterFunction<Throwable, OneParameterAction<Runnable>, NewResult> onFulfilled) {
				this.onFulfilled = onFulfilled;
			}

			@Override
			protected void requestResolution(Throwable throwable) {
				try {
					sendResolution(onFulfilled.resultFrom(throwable, this));
				} catch (Throwable rejection) {
					sendRejection(rejection);
				}
			}
		}
	}

	/**
	 * Created by david on 10/19/16.
	 */
	static final class RejectionDependentExecutor<TResult, TNewRejectedResult> extends ErrorMessenger<TResult, TNewRejectedResult> {
		private final ThreeParameterAction<Throwable, IResolvedPromise<TNewRejectedResult>, IRejectedPromise> onRejected;

		RejectionDependentExecutor(ThreeParameterAction<Throwable, IResolvedPromise<TNewRejectedResult>, IRejectedPromise> onRejected) {
			this.onRejected = onRejected;
		}

		@Override
		protected void requestResolution(Throwable throwable) {
			onRejected.runWith(throwable, this, this);
		}
	}

	/**
	 * Created by david on 10/8/16.
	 */
	static final class ExpectedResultExecutor<TResult, TNewResult> extends ResolutionMessenger<TResult,TNewResult> {
		private final CarelessOneParameterFunction<TResult, TNewResult> onFulfilled;

		ExpectedResultExecutor(CarelessOneParameterFunction<TResult, TNewResult> onFulfilled) {
			this.onFulfilled = onFulfilled;
		}

		@Override
		protected void requestResolution(TResult originalResult) {
			try {
				sendResolution(onFulfilled.resultFrom(originalResult));
			} catch (Throwable rejection) {
				sendRejection(rejection);
			}
		}
	}

	static final class ErrorResultExecutor<TResult, TNewResult> extends ErrorMessenger<TResult, TNewResult> {
		private final CarelessOneParameterFunction<Throwable, TNewResult> onFulfilled;

		ErrorResultExecutor(CarelessOneParameterFunction<Throwable, TNewResult> onFulfilled) {
			this.onFulfilled = onFulfilled;
		}

		@Override
		protected void requestResolution(Throwable throwable) {
			try {
				sendResolution(onFulfilled.resultFrom(throwable));
			} catch (Throwable rejection) {
				sendRejection(rejection);
			}
		}
	}

	/**
	 * Created by david on 10/18/16.
	 */
	static final class ErrorPropagatingResolveExecutor<TResult, TNewResult> extends ResolutionMessenger<TResult, TNewResult> {
		private final ThreeParameterAction<TResult, IResolvedPromise<TNewResult>, IRejectedPromise> onFulfilled;

		ErrorPropagatingResolveExecutor(ThreeParameterAction<TResult, IResolvedPromise<TNewResult>, IRejectedPromise> onFulfilled) {
			this.onFulfilled = onFulfilled;
		}

		@Override
		protected void requestResolution(TResult result) {
			onFulfilled.runWith(result, this, this);
		}
	}
}
