package com.lasthopesoftware.promises;

import com.vedsoft.futures.callables.OneParameterFunction;
import com.vedsoft.futures.callables.TwoParameterFunction;
import com.vedsoft.futures.runnables.FiveParameterAction;
import com.vedsoft.futures.runnables.FourParameterAction;
import com.vedsoft.futures.runnables.OneParameterAction;
import com.vedsoft.futures.runnables.ThreeParameterAction;
import com.vedsoft.futures.runnables.TwoParameterAction;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

	DependentCancellablePromise(@NotNull FiveParameterAction<TInput, Exception, IResolvedPromise<TResult>, IRejectedPromise, OneParameterAction<Runnable>> executor) {
		this.executor = executor;
	}

	void provide(@Nullable TInput input, @Nullable Exception exception) {
		executor.runWith(
			input,
			exception,
			result -> resolve(result, null),
			error -> resolve(null, error),
			cancellation);
	}

	public void cancel() {
		cancellation.cancel();
	}

	private void resolve(TResult result, Exception error) {
		fulfilledResult = result;
		fulfilledError = error;

		isResolved = true;

		for (DependentCancellablePromise<TResult, ?> resolution : resolutions)
			resolution.provide(result, error);
	}

	@NotNull
	private <TNewResult> DependentCancellablePromise<TResult, TNewResult> thenCreateCancellablePromise(@NotNull FiveParameterAction<TResult, Exception, IResolvedPromise<TNewResult>, IRejectedPromise, OneParameterAction<Runnable>> onFulfilled) {
		final DependentCancellablePromise<TResult, TNewResult> newResolution = new DependentCancellablePromise<>(onFulfilled);

		resolutions.add(newResolution);

		if (isResolved)
			newResolution.provide(fulfilledResult, fulfilledError);

		return newResolution;
	}

	@NotNull
	@Override
	public <TNewResult> IPromise<TNewResult> then(@NotNull FourParameterAction<TResult, IResolvedPromise<TNewResult>, IRejectedPromise, OneParameterAction<Runnable>> onFulfilled) {
		return thenCreateCancellablePromise(new Execution.Cancellable.ErrorPropagatingCancellableExecutor<>(onFulfilled));
	}

	@NotNull
	@Override
	public <TNewResult> IPromise<TNewResult> then(@NotNull TwoParameterFunction<TResult, OneParameterAction<Runnable>, TNewResult> onFulfilled) {
		return then(new Execution.Cancellable.ExpectedResultCancellableExecutor<>(onFulfilled));
	}

	@NotNull
	@Override
	public IPromise<Void> then(@NotNull TwoParameterAction<TResult, OneParameterAction<Runnable>> onFulfilled) {
		return then(new Execution.Cancellable.NullReturnCancellableRunnable<>(onFulfilled));
	}

	@NotNull
	@Override
	public <TNewRejectedResult> IPromise<TNewRejectedResult> error(@NotNull FourParameterAction<Exception, IResolvedPromise<TNewRejectedResult>, IRejectedPromise, OneParameterAction<Runnable>> onRejected) {
		return thenCreateCancellablePromise(new Execution.Cancellable.RejectionDependentCancellableExecutor<>(onRejected));
	}

	@NotNull
	@Override
	public <TNewRejectedResult> IPromise<TNewRejectedResult> error(@NotNull TwoParameterFunction<Exception, OneParameterAction<Runnable>, TNewRejectedResult> onRejected) {
		return error(new Execution.Cancellable.ExpectedResultCancellableExecutor<>(onRejected));
	}

	@NotNull
	@Override
	public IPromise<Void> error(@NotNull TwoParameterAction<Exception, OneParameterAction<Runnable>> onRejected) {
		return error(new Execution.Cancellable.NullReturnCancellableRunnable<>(onRejected));
	}

	@NotNull
	private <TNewResult> IPromise<TNewResult> thenCreatePromise(@NotNull FourParameterAction<TResult, Exception, IResolvedPromise<TNewResult>, IRejectedPromise> onFulfilled) {
		return
			thenCreateCancellablePromise(new Execution.NonCancellableExecutor<>(onFulfilled));
	}

	@NotNull
	@Override
	public <TNewResult> IPromise<TNewResult> thenPromise(@NotNull OneParameterFunction<TResult, IPromise<TNewResult>> onFulfilled) {
		return then(new Execution.PromisedResolution<>(onFulfilled));
	}

	@NotNull
	@Override
	public <TNewResult> IPromise<TNewResult> then(@NotNull ThreeParameterAction<TResult, IResolvedPromise<TNewResult>, IRejectedPromise> onFulfilled) {
		return thenCreatePromise(new Execution.ErrorPropagatingResolveExecutor<>(onFulfilled));
	}

	@NotNull
	@Override
	public final <TNewResult> IPromise<TNewResult> then(@NotNull final OneParameterFunction<TResult, TNewResult> onFulfilled) {
		return then(new Execution.ExpectedResultExecutor<>(onFulfilled));
	}

	@NotNull
	@Override
	public final IPromise<Void> then(@NotNull OneParameterAction<TResult> onFulfilled) {
		return then(new Execution.NullReturnRunnable<>(onFulfilled));
	}

	@NotNull
	@Override
	public <TNewRejectedResult> IPromise<TNewRejectedResult> error(@NotNull ThreeParameterAction<Exception, IResolvedPromise<TNewRejectedResult>, IRejectedPromise> onRejected) {
		return thenCreatePromise(new Execution.RejectionDependentExecutor<>(onRejected));
	}

	@NotNull
	@Override
	public <TNewRejectedResult> IPromise<TNewRejectedResult> error(@NotNull OneParameterFunction<Exception, TNewRejectedResult> onRejected) {
		return error(new Execution.ExpectedResultExecutor<>(onRejected));
	}

	@NotNull
	@Override
	public final IPromise<Void> error(@NotNull OneParameterAction<Exception> onRejected) {
		return error(new Execution.NullReturnRunnable<>(onRejected));
	}

	@NotNull
	@Override
	public <TNewResult> IPromise<TNewResult> thenPromise(@NotNull TwoParameterFunction<TResult, OneParameterAction<Runnable>, IPromise<TNewResult>> onFulfilled) {
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
				public void runWith(TResult result, IResolvedPromise<TNewResult> resolve, IRejectedPromise reject, OneParameterAction<Runnable> onCancelled) {
					try {
						resolve.withResult(onFulfilled.expectedUsing(result, onCancelled));
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
				public void runWith(TResult result, Exception exception, IResolvedPromise<TNewResult> resolve, IRejectedPromise reject, OneParameterAction<Runnable> onCancelled) {
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
			static class NullReturnCancellableRunnable<TResult> implements TwoParameterFunction<TResult, OneParameterAction<Runnable>, Void> {
				private final TwoParameterAction<TResult, OneParameterAction<Runnable>> onFulfilled;

				NullReturnCancellableRunnable(TwoParameterAction<TResult, OneParameterAction<Runnable>> onFulfilled) {
					this.onFulfilled = onFulfilled;
				}

				@Override
				public Void expectedUsing(TResult result, OneParameterAction<Runnable> onCancelled) {
					onFulfilled.runWith(result, onCancelled);
					return null;
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
				public void runWith(TResult result, Exception error, IResolvedPromise<TNewRejectedResult> resolve, IRejectedPromise reject, OneParameterAction<Runnable> onCancelled) {
					onRejected.runWith(error, resolve, reject, onCancelled);
				}
			}

			static class ResolvedCancellablePromise<TResult, TNewResult> implements FourParameterAction<TResult, IResolvedPromise<TNewResult>, IRejectedPromise, OneParameterAction<Runnable>> {
				private final TwoParameterFunction<TResult, OneParameterAction<Runnable>, IPromise<TNewResult>> onFulfilled;

				ResolvedCancellablePromise(TwoParameterFunction<TResult, OneParameterAction<Runnable>, IPromise<TNewResult>> onFulfilled) {
					this.onFulfilled = onFulfilled;
				}

				@Override
				public void runWith(TResult result, IResolvedPromise<TNewResult> resolve, IRejectedPromise reject, OneParameterAction<Runnable> onCancelled) {
					try {
						onFulfilled
							.expectedUsing(result, onCancelled)
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
			public void runWith(TResult result, Exception exception, IResolvedPromise<TNewResult> resolve, IRejectedPromise reject, OneParameterAction<Runnable> onCancelled) {
				onFulfilled.runWith(result, exception, resolve, reject);
			}
		}

		/**
		 * Created by david on 10/8/16.
		 */
		static class NullReturnRunnable<TResult> implements OneParameterFunction<TResult, Void> {
			private final OneParameterAction<TResult> resolve;

			NullReturnRunnable(@NotNull OneParameterAction<TResult> resolve) {
				this.resolve = resolve;
			}

			@Override
			public Void expectedUsing(TResult result) {
				resolve.runWith(result);
				return null;
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
			public void runWith(TResult result, Exception exception, IResolvedPromise<TNewRejectedResult> resolve, IRejectedPromise reject) {
				onRejected.runWith(exception, resolve, reject);
			}
		}

		/**
		 * Created by david on 10/8/16.
		 */
		static class ExpectedResultExecutor<TResult, TNewResult> implements ThreeParameterAction<TResult, IResolvedPromise<TNewResult>, IRejectedPromise> {
			private final OneParameterFunction<TResult, TNewResult> onFulfilled;

			ExpectedResultExecutor(@NotNull OneParameterFunction<TResult, TNewResult> onFulfilled) {
				this.onFulfilled = onFulfilled;
			}

			@Override
			public void runWith(TResult originalResult, IResolvedPromise<TNewResult> newResolve, IRejectedPromise newReject) {
				try {
					newResolve.withResult(onFulfilled.expectedUsing(originalResult));
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
			public void runWith(TResult result, Exception exception, IResolvedPromise<TNewResult> resolve, IRejectedPromise reject) {
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
			public void runWith(TResult result, IResolvedPromise<TNewResult> resolve, IRejectedPromise reject) {
				try {
					onFulfilled
						.expectedUsing(result)
						.then(new Resolution.ResolveWithPromiseResult<>(resolve))
						.error(new Resolution.RejectWithPromiseError(reject));
				} catch (Exception e) {
					reject.withError(e);
				}
			}
		}
	}

	private static class Resolution {
		static class ResolveWithPromiseResult<TNewResult> implements OneParameterAction<TNewResult> {
			private final IResolvedPromise<TNewResult> resolve;

			ResolveWithPromiseResult(IResolvedPromise<TNewResult> resolve) {
				this.resolve = resolve;
			}

			@Override
			public void runWith(TNewResult result1) {
				resolve.withResult(result1);
			}
		}

		static class RejectWithPromiseError implements OneParameterAction<Exception> {
			private final IRejectedPromise reject;

			RejectWithPromiseError(IRejectedPromise reject) {
				this.reject = reject;
			}

			@Override
			public void runWith(Exception exception) {
				reject.withError(exception);
			}
		}
	}
}
