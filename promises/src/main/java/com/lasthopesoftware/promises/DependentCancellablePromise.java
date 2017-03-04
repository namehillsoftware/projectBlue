package com.lasthopesoftware.promises;

import com.vedsoft.futures.callables.CarelessOneParameterFunction;
import com.vedsoft.futures.callables.CarelessTwoParameterFunction;
import com.vedsoft.futures.runnables.FiveParameterAction;
import com.vedsoft.futures.runnables.FourParameterAction;
import com.vedsoft.futures.runnables.OneParameterAction;
import com.vedsoft.futures.runnables.ThreeParameterAction;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Created by david on 10/25/16.
 */
class DependentCancellablePromise<TInput, TResult> implements IPromise<TResult> {

	private final FiveParameterAction<TInput, Throwable, IResolvedPromise<TResult>, IRejectedPromise, OneParameterAction<Runnable>> executor;

	private final Queue<DependentCancellablePromise<TResult, ?>> resolutions = new ConcurrentLinkedQueue<>();

	private boolean isResolved;

	private TResult fulfilledResult;
	private Throwable fulfilledError;

	private final Cancellation cancellation = new Cancellation();

	private final ReadWriteLock resolveSync = new ReentrantReadWriteLock();

	DependentCancellablePromise(FiveParameterAction<TInput, Throwable, IResolvedPromise<TResult>, IRejectedPromise, OneParameterAction<Runnable>> executor) {
		this.executor = executor;
	}

	final void provide(TInput input, Throwable exception) {
		executor.runWith(
			input,
			exception,
			result -> resolve(result, null),
			error -> resolve(null, error),
			cancellation);
	}

	public final void cancel() {
		final boolean isResolvedLocally;
		resolveSync.readLock().lock();
		try {
			isResolvedLocally = isResolved;
		} finally {
			resolveSync.readLock().unlock();
		}

		if (!isResolvedLocally)
			cancellation.cancel();
	}

	private void resolve(TResult result, Throwable error) {
		resolveSync.writeLock().lock();
		try {
			if (isResolved) return;

			fulfilledResult = result;
			fulfilledError = error;

			isResolved = true;
		} finally {
			resolveSync.writeLock().unlock();
		}

		processQueue(result, error);
	}

	private void processQueue(TResult result, Throwable error) {
		resolveSync.readLock().lock();
		try {
			if (!isResolved) return;

			while (resolutions.size() > 0) {
				final DependentCancellablePromise<TResult, ?> resolution = resolutions.poll();
				resolution.provide(result, error);
			}
		} finally {
			resolveSync.readLock().unlock();
		}
	}

	private <TNewResult> DependentCancellablePromise<TResult, TNewResult> thenCreateCancellablePromise(FiveParameterAction<TResult, Throwable, IResolvedPromise<TNewResult>, IRejectedPromise, OneParameterAction<Runnable>> onFulfilled) {
		final DependentCancellablePromise<TResult, TNewResult> newResolution = new DependentCancellablePromise<>(onFulfilled);

		resolutions.offer(newResolution);

		processQueue(fulfilledResult, fulfilledError);

		return newResolution;
	}

	@Override
	public final <TNewResult> IPromise<TNewResult> then(FourParameterAction<TResult, IResolvedPromise<TNewResult>, IRejectedPromise, OneParameterAction<Runnable>> onFulfilled) {
		return thenCreateCancellablePromise(new Execution.Cancellable.ErrorPropagatingCancellableExecutor<>(onFulfilled));
	}

	@Override
	public final <TNewResult> IPromise<TNewResult> then(CarelessTwoParameterFunction<TResult, OneParameterAction<Runnable>, TNewResult> onFulfilled) {
		return then(new Execution.Cancellable.ExpectedResultCancellableExecutor<>(onFulfilled));
	}

	@Override
	public final <TNewRejectedResult> IPromise<TNewRejectedResult> error(FourParameterAction<Throwable, IResolvedPromise<TNewRejectedResult>, IRejectedPromise, OneParameterAction<Runnable>> onRejected) {
		return thenCreateCancellablePromise(new Execution.Cancellable.RejectionDependentCancellableExecutor<>(onRejected));
	}

	@Override
	public final <TNewRejectedResult> IPromise<TNewRejectedResult> error(CarelessTwoParameterFunction<Throwable, OneParameterAction<Runnable>, TNewRejectedResult> onRejected) {
		return error(new Execution.Cancellable.ExpectedResultCancellableExecutor<>(onRejected));
	}

	private <TNewResult> IPromise<TNewResult> thenCreatePromise(FourParameterAction<TResult, Throwable, IResolvedPromise<TNewResult>, IRejectedPromise> onFulfilled) {
		return
			thenCreateCancellablePromise(new Execution.NonCancellableExecutor<>(onFulfilled));
	}

	@Override
	public final <TNewResult> IPromise<TNewResult> thenPromise(CarelessOneParameterFunction<TResult, IPromise<TNewResult>> onFulfilled) {
		return then(new Execution.PromisedResolution<>(onFulfilled));
	}

	@Override
	public final <TNewResult> IPromise<TNewResult> then(ThreeParameterAction<TResult, IResolvedPromise<TNewResult>, IRejectedPromise> onFulfilled) {
		return thenCreatePromise(new Execution.ErrorPropagatingResolveExecutor<>(onFulfilled));
	}

	@Override
	public final <TNewResult> IPromise<TNewResult> then(final CarelessOneParameterFunction<TResult, TNewResult> onFulfilled) {
		return then(new Execution.ExpectedResultExecutor<>(onFulfilled));
	}

	@Override
	public final <TNewRejectedResult> IPromise<TNewRejectedResult> error(ThreeParameterAction<Throwable, IResolvedPromise<TNewRejectedResult>, IRejectedPromise> onRejected) {
		return thenCreatePromise(new Execution.RejectionDependentExecutor<>(onRejected));
	}

	@Override
	public final <TNewRejectedResult> IPromise<TNewRejectedResult> error(CarelessOneParameterFunction<Throwable, TNewRejectedResult> onRejected) {
		return error(new Execution.ExpectedResultExecutor<>(onRejected));
	}

	@Override
	public final <TNewResult> IPromise<TNewResult> thenPromise(CarelessTwoParameterFunction<TResult, OneParameterAction<Runnable>, IPromise<TNewResult>> onFulfilled) {
		return then(new Execution.Cancellable.ResolvedCancellablePromise<>(onFulfilled));
	}

	private static class Execution {
		private static class Cancellable  {

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
						resolve.withResult(onFulfilled.resultFrom(result, onCancelled));
					} catch (Exception e) {
						reject.withError(e);
					}
				}
			}

			/**
			 * Created by david on 10/30/16.
			 */
			static class ErrorPropagatingCancellableExecutor<TResult, TNewResult> implements FiveParameterAction<TResult, Throwable, IResolvedPromise<TNewResult>, IRejectedPromise, OneParameterAction<Runnable>> {
				private final FourParameterAction<TResult, IResolvedPromise<TNewResult>, IRejectedPromise, OneParameterAction<Runnable>> onFulfilled;

				ErrorPropagatingCancellableExecutor(FourParameterAction<TResult, IResolvedPromise<TNewResult>, IRejectedPromise, OneParameterAction<Runnable>> onFulfilled) {
					this.onFulfilled = onFulfilled;
				}

				@Override
				public final void runWith(TResult result, Throwable exception, IResolvedPromise<TNewResult> resolve, IRejectedPromise reject, OneParameterAction<Runnable> onCancelled) {
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
			static class RejectionDependentCancellableExecutor<TResult, TNewRejectedResult> implements FiveParameterAction<TResult, Throwable, IResolvedPromise<TNewRejectedResult>, IRejectedPromise, OneParameterAction<Runnable>> {
				private final FourParameterAction<Throwable, IResolvedPromise<TNewRejectedResult>, IRejectedPromise, OneParameterAction<Runnable>> onRejected;

				RejectionDependentCancellableExecutor(FourParameterAction<Throwable, IResolvedPromise<TNewRejectedResult>, IRejectedPromise, OneParameterAction<Runnable>> onRejected) {
					this.onRejected = onRejected;
				}

				@Override
				public final void runWith(TResult result, Throwable error, IResolvedPromise<TNewRejectedResult> resolve, IRejectedPromise reject, OneParameterAction<Runnable> onCancelled) {
					if (error != null)
						onRejected.runWith(error, resolve, reject, onCancelled);
				}
			}

			static class ResolvedCancellablePromise<TResult, TNewResult> implements FourParameterAction<TResult, IResolvedPromise<TNewResult>, IRejectedPromise, OneParameterAction<Runnable>> {
				private final CarelessTwoParameterFunction<TResult, OneParameterAction<Runnable>, IPromise<TNewResult>> onFulfilled;

				ResolvedCancellablePromise(CarelessTwoParameterFunction<TResult, OneParameterAction<Runnable>, IPromise<TNewResult>> onFulfilled) {
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
		static class NonCancellableExecutor<TResult, TNewResult> implements FiveParameterAction<TResult, Throwable, IResolvedPromise<TNewResult>, IRejectedPromise, OneParameterAction<Runnable>> {
			private final FourParameterAction<TResult, Throwable, IResolvedPromise<TNewResult>, IRejectedPromise> onFulfilled;

			NonCancellableExecutor(FourParameterAction<TResult, Throwable, IResolvedPromise<TNewResult>, IRejectedPromise> onFulfilled) {
				this.onFulfilled = onFulfilled;
			}

			@Override
			public final void runWith(TResult result, Throwable exception, IResolvedPromise<TNewResult> resolve, IRejectedPromise reject, OneParameterAction<Runnable> onCancelled) {
				onFulfilled.runWith(result, exception, resolve, reject);
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
					newResolve.withResult(onFulfilled.resultFrom(originalResult));
				} catch (Exception e) {
					newReject.withError(e);
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
					reject.withError(exception);
					return;
				}

				onFulfilled.runWith(result, resolve, reject);
			}
		}

		static class PromisedResolution<TResult, TNewResult> implements ThreeParameterAction<TResult, IResolvedPromise<TNewResult>, IRejectedPromise> {
			private final CarelessOneParameterFunction<TResult, IPromise<TNewResult>> onFulfilled;

			PromisedResolution(CarelessOneParameterFunction<TResult, IPromise<TNewResult>> onFulfilled) {
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
		static class ResolveWithPromiseResult<TNewResult> implements CarelessOneParameterFunction<TNewResult, Void> {
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

		static class RejectWithPromiseError implements CarelessOneParameterFunction<Throwable, Void> {
			private final IRejectedPromise reject;

			RejectWithPromiseError(IRejectedPromise reject) {
				this.reject = reject;
			}

			@Override
			public final Void resultFrom(Throwable exception) {
				reject.withError(exception);
				return null;
			}
		}
	}
}
