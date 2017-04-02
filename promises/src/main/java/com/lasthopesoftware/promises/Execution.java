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
class Execution {

	static class InternalCancellablePromiseExecutor<TResult> extends EmptyMessenger<TResult> {
		private final ThreeParameterAction<IResolvedPromise<TResult>, IRejectedPromise, OneParameterAction<Runnable>> executor;

		InternalCancellablePromiseExecutor(ThreeParameterAction<IResolvedPromise<TResult>, IRejectedPromise, OneParameterAction<Runnable>> executor) {
			this.executor = executor;
		}

		@Override
		protected void requestResolution() {
			executor.runWith(this, this, this);
		}
	}

	/**
	 * Created by david on 10/8/16.
	 */
	static class InternalPromiseExecutor<TResult> implements ThreeParameterAction<IResolvedPromise<TResult>, IRejectedPromise, OneParameterAction<Runnable>> {
		private final TwoParameterAction<IResolvedPromise<TResult>, IRejectedPromise> executor;

		InternalPromiseExecutor(TwoParameterAction<IResolvedPromise<TResult>, IRejectedPromise> executor) {
			this.executor = executor;
		}

		@Override
		public void runWith(IResolvedPromise<TResult> resolve, IRejectedPromise reject, OneParameterAction<Runnable> onCancelled) {
			executor.runWith(resolve, reject);
		}
	}

	static class InternalExpectedPromiseExecutor<TResult> implements TwoParameterAction<IResolvedPromise<TResult>, IRejectedPromise> {
		private final CarelessFunction<TResult> executor;

		InternalExpectedPromiseExecutor(CarelessFunction<TResult> executor) {
			this.executor = executor;
		}

		@Override
		public void runWith(IResolvedPromise<TResult> resolve, IRejectedPromise reject) {
			try {
				resolve.sendResolution(executor.result());
			} catch (Exception e) {
				reject.sendRejection(e);
			}
		}
	}

	static class PassThroughCallable<TPassThroughResult> implements CarelessFunction<TPassThroughResult> {
		private final TPassThroughResult passThroughResult;

		PassThroughCallable(TPassThroughResult passThroughResult) {
			this.passThroughResult = passThroughResult;
		}

		@Override
		public TPassThroughResult result() throws Exception {
			return passThroughResult;
		}
	}

	static class Cancellable {

		/**
		 * Created by david on 10/30/16.
		 */
		static class ExpectedResultCancellableExecutor<TResult, TNewResult> implements FourParameterAction<TResult, IResolvedPromise<TNewResult>, IRejectedPromise, OneParameterAction<Runnable>> {
			private final CarelessTwoParameterFunction<TResult, OneParameterAction<Runnable>, TNewResult> onFulfilled;

			ExpectedResultCancellableExecutor(CarelessTwoParameterFunction<TResult, OneParameterAction<Runnable>, TNewResult> onFulfilled) {
				this.onFulfilled = onFulfilled;
			}

			@Override
			public final void runWith(TResult result, IResolvedPromise<TNewResult> resolve, IRejectedPromise reject, OneParameterAction<Runnable> onCancelled) {
				try {
					resolve.sendResolution(onFulfilled.resultFrom(result, onCancelled));
				} catch (Exception e) {
					reject.sendRejection(e);
				}
			}
		}

		/**
		 * Created by david on 10/30/16.
		 */
		static class ErrorPropagatingCancellableExecutor<TResult, TNewResult> extends Messenger<TResult, TNewResult> {
			private final FourParameterAction<TResult, IResolvedPromise<TNewResult>, IRejectedPromise, OneParameterAction<Runnable>> onFulfilled;

			ErrorPropagatingCancellableExecutor(FourParameterAction<TResult, IResolvedPromise<TNewResult>, IRejectedPromise, OneParameterAction<Runnable>> onFulfilled) {
				this.onFulfilled = onFulfilled;
			}


			@Override
			public void requestResolution(TResult result, Throwable exception) {
				if (exception != null) {
					sendRejection(exception);
					return;
				}

				onFulfilled.runWith(
					result,
					this,
					this,
					this);
			}
		}

		/**
		 * Created by david on 10/30/16.
		 */
		static class RejectionDependentCancellableExecutor<TResult, TNewRejectedResult> extends Messenger<TResult, TNewRejectedResult> {
			private final FourParameterAction<Throwable, IResolvedPromise<TNewRejectedResult>, IRejectedPromise, OneParameterAction<Runnable>> onRejected;

			RejectionDependentCancellableExecutor(FourParameterAction<Throwable, IResolvedPromise<TNewRejectedResult>, IRejectedPromise, OneParameterAction<Runnable>> onRejected) {
				this.onRejected = onRejected;
			}

			@Override
			public void requestResolution(TResult result, Throwable throwable) {
				if (throwable != null)
					onRejected.runWith(throwable, this, this, this);
			}
		}

		static class ResolvedCancellablePromise<TResult, TNewResult> implements FourParameterAction<TResult, IResolvedPromise<TNewResult>, IRejectedPromise, OneParameterAction<Runnable>> {
			private final CarelessTwoParameterFunction<TResult, OneParameterAction<Runnable>, Promise<TNewResult>> onFulfilled;

			ResolvedCancellablePromise(CarelessTwoParameterFunction<TResult, OneParameterAction<Runnable>, Promise<TNewResult>> onFulfilled) {
				this.onFulfilled = onFulfilled;
			}

			@Override
			public final void runWith(TResult result, IResolvedPromise<TNewResult> resolve, IRejectedPromise reject, OneParameterAction<Runnable> onCancelled) {
				try {
					onFulfilled
						.resultFrom(result, onCancelled)
						.then(new Resolution.ResolveWithPromiseResult<>(resolve))
						.error(new Resolution.RejectWithPromiseError(reject));
				} catch (Exception e) {
					reject.sendRejection(e);
				}
			}
		}
	}

	/**
	 * Created by david on 10/30/16.
	 */
	static class NonCancellableExecutor<TResult, TNewResult> extends Messenger<TResult, TNewResult> {
		private final FourParameterAction<TResult, Throwable, IResolvedPromise<TNewResult>, IRejectedPromise> onFulfilled;

		NonCancellableExecutor(FourParameterAction<TResult, Throwable, IResolvedPromise<TNewResult>, IRejectedPromise> onFulfilled) {
			this.onFulfilled = onFulfilled;
		}

		@Override
		public void requestResolution(TResult result, Throwable throwable) {
			onFulfilled.runWith(result, throwable, this, this);
		}
	}

	/**
	 * Created by david on 10/19/16.
	 */
	static class RejectionDependentExecutor<TResult, TNewRejectedResult> implements FourParameterAction<TResult, Throwable, IResolvedPromise<TNewRejectedResult>, IRejectedPromise> {
		private final ThreeParameterAction<Throwable, IResolvedPromise<TNewRejectedResult>, IRejectedPromise> onRejected;

		RejectionDependentExecutor(ThreeParameterAction<Throwable, IResolvedPromise<TNewRejectedResult>, IRejectedPromise> onRejected) {
			this.onRejected = onRejected;
		}

		@Override
		public final void runWith(TResult result, Throwable exception, IResolvedPromise<TNewRejectedResult> resolve, IRejectedPromise reject) {
			if (exception != null)
				onRejected.runWith(exception, resolve, reject);
		}
	}

	/**
	 * Created by david on 10/8/16.
	 */
	static class ExpectedResultExecutor<TResult, TNewResult> implements ThreeParameterAction<TResult, IResolvedPromise<TNewResult>, IRejectedPromise> {
		private final CarelessOneParameterFunction<TResult, TNewResult> onFulfilled;

		ExpectedResultExecutor(CarelessOneParameterFunction<TResult, TNewResult> onFulfilled) {
			this.onFulfilled = onFulfilled;
		}

		@Override
		public final void runWith(TResult originalResult, IResolvedPromise<TNewResult> newResolve, IRejectedPromise newReject) {
			try {
				newResolve.sendResolution(onFulfilled.resultFrom(originalResult));
			} catch (Exception e) {
				newReject.sendRejection(e);
			}
		}
	}

	/**
	 * Created by david on 10/18/16.
	 */
	static class ErrorPropagatingResolveExecutor<TResult, TNewResult> implements FourParameterAction<TResult, Throwable, IResolvedPromise<TNewResult>, IRejectedPromise> {
		private final ThreeParameterAction<TResult, IResolvedPromise<TNewResult>, IRejectedPromise> onFulfilled;

		ErrorPropagatingResolveExecutor(ThreeParameterAction<TResult, IResolvedPromise<TNewResult>, IRejectedPromise> onFulfilled) {
			this.onFulfilled = onFulfilled;
		}

		@Override
		public final void runWith(TResult result, Throwable exception, IResolvedPromise<TNewResult> resolve, IRejectedPromise reject) {
			if (exception != null) {
				reject.sendRejection(exception);
				return;
			}

			onFulfilled.runWith(result, resolve, reject);
		}
	}

	static class PromisedResolution<TResult, TNewResult> implements ThreeParameterAction<TResult, IResolvedPromise<TNewResult>, IRejectedPromise> {
		private final CarelessOneParameterFunction<TResult, Promise<TNewResult>> onFulfilled;

		PromisedResolution(CarelessOneParameterFunction<TResult, Promise<TNewResult>> onFulfilled) {
			this.onFulfilled = onFulfilled;
		}

		@Override
		public final void runWith(TResult result, IResolvedPromise<TNewResult> resolve, IRejectedPromise reject) {
			try {
				onFulfilled
					.resultFrom(result)
					.then(new Resolution.ResolveWithPromiseResult<>(resolve))
					.error(new Resolution.RejectWithPromiseError(reject));
			} catch (Exception e) {
				reject.sendRejection(e);
			}
		}
	}
}
