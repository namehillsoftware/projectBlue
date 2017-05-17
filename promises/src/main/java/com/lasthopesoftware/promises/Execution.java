package com.lasthopesoftware.promises;

import com.vedsoft.futures.callables.CarelessFunction;
import com.vedsoft.futures.callables.CarelessOneParameterFunction;
import com.vedsoft.futures.callables.CarelessTwoParameterFunction;
import com.vedsoft.futures.runnables.FourParameterAction;
import com.vedsoft.futures.runnables.OneParameterAction;
import com.vedsoft.futures.runnables.ThreeParameterAction;
import com.vedsoft.futures.runnables.TwoParameterAction;

/**
 * Created by david on 4/2/17.
 */
final class Execution {

	static final class InternalCancellablePromiseExecutor<TResult> extends EmptyMessenger<TResult> {
		private final ThreeParameterAction<IResolvedPromise<TResult>, IRejectedPromise, OneParameterAction<Runnable>> executor;

		InternalCancellablePromiseExecutor(ThreeParameterAction<IResolvedPromise<TResult>, IRejectedPromise, OneParameterAction<Runnable>> executor) {
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
	static final class InternalPromiseExecutor<TResult> implements ThreeParameterAction<IResolvedPromise<TResult>, IRejectedPromise, OneParameterAction<Runnable>> {
		private final TwoParameterAction<IResolvedPromise<TResult>, IRejectedPromise> executor;

		InternalPromiseExecutor(TwoParameterAction<IResolvedPromise<TResult>, IRejectedPromise> executor) {
			this.executor = executor;
		}

		@Override
		public void runWith(IResolvedPromise<TResult> resolve, IRejectedPromise reject, OneParameterAction<Runnable> onCancelled) {
			executor.runWith(resolve, reject);
		}
	}

	static final class InternalExpectedPromiseExecutor<TResult> implements TwoParameterAction<IResolvedPromise<TResult>, IRejectedPromise> {
		private final CarelessFunction<TResult> executor;

		InternalExpectedPromiseExecutor(CarelessFunction<TResult> executor) {
			this.executor = executor;
		}

		@Override
		public void runWith(IResolvedPromise<TResult> resolve, IRejectedPromise reject) {
			try {
				resolve.sendResolution(executor.result());
			} catch (Throwable rejection) {
				reject.sendRejection(rejection);
			}
		}
	}

	static final class PassThroughCallable<TPassThroughResult> implements CarelessFunction<TPassThroughResult> {
		private final TPassThroughResult passThroughResult;

		PassThroughCallable(TPassThroughResult passThroughResult) {
			this.passThroughResult = passThroughResult;
		}

		@Override
		public TPassThroughResult result() throws Exception {
			return passThroughResult;
		}
	}

	static final class Cancellable {

		/**
		 * Created by david on 10/30/16.
		 */
		static final class ExpectedResultCancellableExecutor<TResult, TNewResult> implements FourParameterAction<TResult, IResolvedPromise<TNewResult>, IRejectedPromise, OneParameterAction<Runnable>> {
			private final CarelessTwoParameterFunction<TResult, OneParameterAction<Runnable>, TNewResult> onFulfilled;

			ExpectedResultCancellableExecutor(CarelessTwoParameterFunction<TResult, OneParameterAction<Runnable>, TNewResult> onFulfilled) {
				this.onFulfilled = onFulfilled;
			}

			@Override
			public final void runWith(TResult result, IResolvedPromise<TNewResult> resolve, IRejectedPromise reject, OneParameterAction<Runnable> onCancelled) {
				try {
					resolve.sendResolution(onFulfilled.resultFrom(result, onCancelled));
				} catch (Throwable rejection) {
					reject.sendRejection(rejection);
				}
			}
		}

		/**
		 * Created by david on 10/30/16.
		 */
		static final class ErrorPropagatingCancellableExecutor<TResult, TNewResult> extends Messenger<TResult, TNewResult> {
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
		static final class RejectionDependentCancellableExecutor<TNewRejectedResult> extends Messenger<Throwable, TNewRejectedResult> {
			private final FourParameterAction<Throwable, IResolvedPromise<TNewRejectedResult>, IRejectedPromise, OneParameterAction<Runnable>> onRejected;

			RejectionDependentCancellableExecutor(FourParameterAction<Throwable, IResolvedPromise<TNewRejectedResult>, IRejectedPromise, OneParameterAction<Runnable>> onRejected) {
				this.onRejected = onRejected;
			}

			@Override
			protected void requestResolution(Throwable throwable) {
				onRejected.runWith(throwable, this, this, this);
			}
		}
	}

	/**
	 * Created by david on 10/19/16.
	 */
	static final class RejectionDependentExecutor<TNewRejectedResult> extends Messenger<Throwable, TNewRejectedResult> {
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
	static final class ExpectedResultExecutor<TResult, TNewResult> implements ThreeParameterAction<TResult, IResolvedPromise<TNewResult>, IRejectedPromise> {
		private final CarelessOneParameterFunction<TResult, TNewResult> onFulfilled;

		ExpectedResultExecutor(CarelessOneParameterFunction<TResult, TNewResult> onFulfilled) {
			this.onFulfilled = onFulfilled;
		}

		@Override
		public final void runWith(TResult originalResult, IResolvedPromise<TNewResult> newResolve, IRejectedPromise newReject) {
			try {
				newResolve.sendResolution(onFulfilled.resultFrom(originalResult));
			} catch (Throwable rejection) {
				newReject.sendRejection(rejection);
			}
		}
	}

	/**
	 * Created by david on 10/18/16.
	 */
	static final class ErrorPropagatingResolveExecutor<TResult, TNewResult> extends Messenger<TResult, TNewResult> {
		private final ThreeParameterAction<TResult, IResolvedPromise<TNewResult>, IRejectedPromise> onFulfilled;

		ErrorPropagatingResolveExecutor(ThreeParameterAction<TResult, IResolvedPromise<TNewResult>, IRejectedPromise> onFulfilled) {
			this.onFulfilled = onFulfilled;
		}

		@Override
		protected void requestResolution(TResult result) {
			onFulfilled.runWith(result, this, this);
		}
	}

	static final class PromisedResolution<TResult, TNewResult> implements ThreeParameterAction<TResult, IResolvedPromise<TNewResult>, IRejectedPromise> {
		private final ProducePromise<TResult, TNewResult> onFulfilled;

		PromisedResolution(ProducePromise<TResult, TNewResult> onFulfilled) {
			this.onFulfilled = onFulfilled;
		}

		@Override
		public final void runWith(TResult result, IResolvedPromise<TNewResult> resolve, IRejectedPromise reject) {
			try {
				onFulfilled
					.producePromise(result)
					.then(new Resolution.ResolveWithPromiseResult<>(resolve))
					.error(new Resolution.RejectWithPromiseError(reject));
			} catch (Throwable rejection) {
				reject.sendRejection(rejection);
			}
		}
	}
}
