package com.lasthopesoftware.promises;

import com.vedsoft.futures.callables.OneParameterFunction;
import com.vedsoft.futures.callables.TwoParameterFunction;
import com.vedsoft.futures.runnables.FiveParameterAction;
import com.vedsoft.futures.runnables.FourParameterAction;
import com.vedsoft.futures.runnables.OneParameterAction;
import com.vedsoft.futures.runnables.ThreeParameterAction;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by david on 10/25/16.
 */
class DependentCancellablePromise<TInput, TResult> implements IPromise<TResult> {

	private final FiveParameterAction<TInput, Exception, IResolvedPromise<TResult>, IRejectedPromise, OneParameterAction<Runnable>> executor;

	private final List<DependentCancellablePromise<TResult, ?>> resolutions = new ArrayList<>();

	private volatile boolean isResolved;

	private TResult fulfilledResult;
	private Exception fulfilledError;

	private final Cancellation cancellation = new Cancellation();

	DependentCancellablePromise(FiveParameterAction<TInput, Exception, IResolvedPromise<TResult>, IRejectedPromise, OneParameterAction<Runnable>> executor) {
		this.executor = executor;
	}

	final void provide(TInput input, Exception exception) {
		executor.runWith(
			input,
			exception,
			result -> resolve(result, null),
			error -> resolve(null, error),
			cancellation);
	}

	public final void cancel() {
		cancellation.cancel();
	}

	private void resolve(TResult result, Exception error) {
		fulfilledResult = result;
		fulfilledError = error;

		isResolved = true;

		for (DependentCancellablePromise<TResult, ?> resolution : resolutions)
			resolution.provide(result, error);
	}

	private <TNewResult> DependentCancellablePromise<TResult, TNewResult> thenCreateCancellablePromise(FiveParameterAction<TResult, Exception, IResolvedPromise<TNewResult>, IRejectedPromise, OneParameterAction<Runnable>> onFulfilled) {
		final DependentCancellablePromise<TResult, TNewResult> newResolution = new DependentCancellablePromise<>(onFulfilled);

		resolutions.add(newResolution);

		if (isResolved)
			newResolution.provide(fulfilledResult, fulfilledError);

		return newResolution;
	}

	@Override
	public final <TNewResult> IPromise<TNewResult> then(FourParameterAction<TResult, IResolvedPromise<TNewResult>, IRejectedPromise, OneParameterAction<Runnable>> onFulfilled) {
		return thenCreateCancellablePromise(new Execution.Cancellable.ErrorPropagatingCancellableExecutor<>(onFulfilled));
	}

	@Override
	public final <TNewResult> IPromise<TNewResult> then(TwoParameterFunction<TResult, OneParameterAction<Runnable>, TNewResult> onFulfilled) {
		return then(new Execution.Cancellable.ExpectedResultCancellableExecutor<>(onFulfilled));
	}

	@Override
	public final <TNewRejectedResult> IPromise<TNewRejectedResult> error(FourParameterAction<Exception, IResolvedPromise<TNewRejectedResult>, IRejectedPromise, OneParameterAction<Runnable>> onRejected) {
		return thenCreateCancellablePromise(new Execution.Cancellable.RejectionDependentCancellableExecutor<>(onRejected));
	}

	@Override
	public final <TNewRejectedResult> IPromise<TNewRejectedResult> error(TwoParameterFunction<Exception, OneParameterAction<Runnable>, TNewRejectedResult> onRejected) {
		return error(new Execution.Cancellable.ExpectedResultCancellableExecutor<>(onRejected));
	}

	private <TNewResult> IPromise<TNewResult> thenCreatePromise(FourParameterAction<TResult, Exception, IResolvedPromise<TNewResult>, IRejectedPromise> onFulfilled) {
		return
			thenCreateCancellablePromise(new Execution.NonCancellableExecutor<>(onFulfilled));
	}

	@Override
	public final <TNewResult> IPromise<TNewResult> thenPromise(OneParameterFunction<TResult, IPromise<TNewResult>> onFulfilled) {
		return then(new Execution.PromisedResolution<>(onFulfilled));
	}

	@Override
	public final <TNewResult> IPromise<TNewResult> then(ThreeParameterAction<TResult, IResolvedPromise<TNewResult>, IRejectedPromise> onFulfilled) {
		return thenCreatePromise(new Execution.ErrorPropagatingResolveExecutor<>(onFulfilled));
	}

	@Override
	public final <TNewResult> IPromise<TNewResult> then(final OneParameterFunction<TResult, TNewResult> onFulfilled) {
		return then(new Execution.ExpectedResultExecutor<>(onFulfilled));
	}

	@Override
	public final <TNewRejectedResult> IPromise<TNewRejectedResult> error(ThreeParameterAction<Exception, IResolvedPromise<TNewRejectedResult>, IRejectedPromise> onRejected) {
		return thenCreatePromise(new Execution.RejectionDependentExecutor<>(onRejected));
	}

	@Override
	public final <TNewRejectedResult> IPromise<TNewRejectedResult> error(OneParameterFunction<Exception, TNewRejectedResult> onRejected) {
		return error(new Execution.ExpectedResultExecutor<>(onRejected));
	}

	@Override
	public final <TNewResult> IPromise<TNewResult> thenPromise(TwoParameterFunction<TResult, OneParameterAction<Runnable>, IPromise<TNewResult>> onFulfilled) {
		return then(new Execution.Cancellable.ResolvedCancellablePromise<>(onFulfilled));
	}

	private static class Execution {
		private static class Cancellable  {

			/**
			 * Created by david on 10/30/16.
			 */
			static class ExpectedResultCancellableExecutor<TResult, TNewResult> implements FourParameterAction<TResult, IResolvedPromise<TNewResult>, IRejectedPromise, OneParameterAction<Runnable>> {
				private final TwoParameterFunction<TResult, OneParameterAction<Runnable>, TNewResult> onFulfilled;

				ExpectedResultCancellableExecutor(TwoParameterFunction<TResult, OneParameterAction<Runnable>, TNewResult> onFulfilled) {
					this.onFulfilled = onFulfilled;
				}

				@Override
				public final void runWith(TResult result, IResolvedPromise<TNewResult> resolve, IRejectedPromise reject, OneParameterAction<Runnable> onCancelled) {
					try {
						resolve.withResult(onFulfilled.resultFrom(result, onCancelled));
					} catch (Exception e) {
						reject.withError(e);
					}
				}
			}

			/**
			 * Created by david on 10/30/16.
			 */
			static class ErrorPropagatingCancellableExecutor<TResult, TNewResult> implements FiveParameterAction<TResult, Exception, IResolvedPromise<TNewResult>, IRejectedPromise, OneParameterAction<Runnable>> {
				private final FourParameterAction<TResult, IResolvedPromise<TNewResult>, IRejectedPromise, OneParameterAction<Runnable>> onFulfilled;

				ErrorPropagatingCancellableExecutor(FourParameterAction<TResult, IResolvedPromise<TNewResult>, IRejectedPromise, OneParameterAction<Runnable>> onFulfilled) {
					this.onFulfilled = onFulfilled;
				}

				@Override
				public final void runWith(TResult result, Exception exception, IResolvedPromise<TNewResult> resolve, IRejectedPromise reject, OneParameterAction<Runnable> onCancelled) {
					if (exception != null) {
						reject.withError(exception);
						return;
					}

					onFulfilled.runWith(result, resolve, reject, onCancelled);
				}
			}

			/**
			 * Created by david on 10/30/16.
			 */
			static class RejectionDependentCancellableExecutor<TResult, TNewRejectedResult> implements FiveParameterAction<TResult, Exception, IResolvedPromise<TNewRejectedResult>, IRejectedPromise, OneParameterAction<Runnable>> {
				private final FourParameterAction<Exception, IResolvedPromise<TNewRejectedResult>, IRejectedPromise, OneParameterAction<Runnable>> onRejected;

				RejectionDependentCancellableExecutor(FourParameterAction<Exception, IResolvedPromise<TNewRejectedResult>, IRejectedPromise, OneParameterAction<Runnable>> onRejected) {
					this.onRejected = onRejected;
				}

				@Override
				public final void runWith(TResult result, Exception error, IResolvedPromise<TNewRejectedResult> resolve, IRejectedPromise reject, OneParameterAction<Runnable> onCancelled) {
					if (error != null)
						onRejected.runWith(error, resolve, reject, onCancelled);
				}
			}

			static class ResolvedCancellablePromise<TResult, TNewResult> implements FourParameterAction<TResult, IResolvedPromise<TNewResult>, IRejectedPromise, OneParameterAction<Runnable>> {
				private final TwoParameterFunction<TResult, OneParameterAction<Runnable>, IPromise<TNewResult>> onFulfilled;

				ResolvedCancellablePromise(TwoParameterFunction<TResult, OneParameterAction<Runnable>, IPromise<TNewResult>> onFulfilled) {
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
						reject.withError(e);
					}
				}
			}
		}

		/**
		 * Created by david on 10/30/16.
		 */
		static class NonCancellableExecutor<TResult, TNewResult> implements FiveParameterAction<TResult, Exception, IResolvedPromise<TNewResult>, IRejectedPromise, OneParameterAction<Runnable>> {
			private final FourParameterAction<TResult, Exception, IResolvedPromise<TNewResult>, IRejectedPromise> onFulfilled;

			NonCancellableExecutor(FourParameterAction<TResult, Exception, IResolvedPromise<TNewResult>, IRejectedPromise> onFulfilled) {
				this.onFulfilled = onFulfilled;
			}

			@Override
			public final void runWith(TResult result, Exception exception, IResolvedPromise<TNewResult> resolve, IRejectedPromise reject, OneParameterAction<Runnable> onCancelled) {
				onFulfilled.runWith(result, exception, resolve, reject);
			}
		}

		/**
		 * Created by david on 10/19/16.
		 */
		static class RejectionDependentExecutor<TResult, TNewRejectedResult> implements FourParameterAction<TResult, Exception, IResolvedPromise<TNewRejectedResult>, IRejectedPromise> {
			private final ThreeParameterAction<Exception, IResolvedPromise<TNewRejectedResult>, IRejectedPromise> onRejected;

			RejectionDependentExecutor(ThreeParameterAction<Exception, IResolvedPromise<TNewRejectedResult>, IRejectedPromise> onRejected) {
				this.onRejected = onRejected;
			}

			@Override
			public final void runWith(TResult result, Exception exception, IResolvedPromise<TNewRejectedResult> resolve, IRejectedPromise reject) {
				if (exception != null)
					onRejected.runWith(exception, resolve, reject);
			}
		}

		/**
		 * Created by david on 10/8/16.
		 */
		static class ExpectedResultExecutor<TResult, TNewResult> implements ThreeParameterAction<TResult, IResolvedPromise<TNewResult>, IRejectedPromise> {
			private final OneParameterFunction<TResult, TNewResult> onFulfilled;

			ExpectedResultExecutor(OneParameterFunction<TResult, TNewResult> onFulfilled) {
				this.onFulfilled = onFulfilled;
			}

			@Override
			public final void runWith(TResult originalResult, IResolvedPromise<TNewResult> newResolve, IRejectedPromise newReject) {
				try {
					newResolve.withResult(onFulfilled.resultFrom(originalResult));
				} catch (Exception e) {
					newReject.withError(e);
				}
			}
		}

		/**
		 * Created by david on 10/18/16.
		 */
		static class ErrorPropagatingResolveExecutor<TResult, TNewResult> implements FourParameterAction<TResult, Exception, IResolvedPromise<TNewResult>, IRejectedPromise> {
			private final ThreeParameterAction<TResult, IResolvedPromise<TNewResult>, IRejectedPromise> onFulfilled;

			ErrorPropagatingResolveExecutor(ThreeParameterAction<TResult, IResolvedPromise<TNewResult>, IRejectedPromise> onFulfilled) {
				this.onFulfilled = onFulfilled;
			}

			@Override
			public final void runWith(TResult result, Exception exception, IResolvedPromise<TNewResult> resolve, IRejectedPromise reject) {
				if (exception != null) {
					reject.withError(exception);
					return;
				}

				onFulfilled.runWith(result, resolve, reject);
			}
		}

		static class PromisedResolution<TResult, TNewResult> implements ThreeParameterAction<TResult, IResolvedPromise<TNewResult>, IRejectedPromise> {
			private final OneParameterFunction<TResult, IPromise<TNewResult>> onFulfilled;

			PromisedResolution(OneParameterFunction<TResult, IPromise<TNewResult>> onFulfilled) {
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
					reject.withError(e);
				}
			}
		}
	}

	private static class Resolution {
		static class ResolveWithPromiseResult<TNewResult> implements OneParameterFunction<TNewResult, Void> {
			private final IResolvedPromise<TNewResult> resolve;

			ResolveWithPromiseResult(IResolvedPromise<TNewResult> resolve) {
				this.resolve = resolve;
			}

			@Override
			public final Void resultFrom(TNewResult result) {
				resolve.withResult(result);
				return null;
			}
		}

		static class RejectWithPromiseError implements OneParameterFunction<Exception, Void> {
			private final IRejectedPromise reject;

			RejectWithPromiseError(IRejectedPromise reject) {
				this.reject = reject;
			}

			@Override
			public final Void resultFrom(Exception exception) {
				reject.withError(exception);
				return null;
			}
		}
	}
}
