package com.lasthopesoftware.promises;

import com.vedsoft.futures.callables.CarelessFunction;
import com.vedsoft.futures.callables.CarelessOneParameterFunction;
import com.vedsoft.futures.callables.CarelessTwoParameterFunction;
import com.vedsoft.futures.runnables.FourParameterAction;
import com.vedsoft.futures.runnables.OneParameterAction;
import com.vedsoft.futures.runnables.ThreeParameterAction;
import com.vedsoft.futures.runnables.TwoParameterAction;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

public class Promise<TResult> implements IPromise<TResult> {

	private final Messenger<?, TResult> messenger;

	public Promise(ThreeParameterAction<IResolvedPromise<TResult>, IRejectedPromise, OneParameterAction<Runnable>> executor) {
		this(new Execution.InternalCancellablePromiseExecutor<>(executor));
	}

	public Promise(TwoParameterAction<IResolvedPromise<TResult>, IRejectedPromise> executor) {
		this(new Execution.InternalPromiseExecutor<>(executor));
	}

	public Promise(CarelessFunction<TResult> executor) {
		this(new Execution.InternalExpectedPromiseExecutor<>(executor));
	}

	public Promise(TResult passThroughResult) {
		this(new Execution.PassThroughCallable<>(passThroughResult));
	}

	private Promise(Messenger<?, TResult> messenger) {
		this.messenger = messenger;
	}

	public void cancel() {
		messenger.cancel();
	}

	private <TNewResult> Promise<TNewResult> thenCreateCancellablePromise(Messenger<TResult, TNewResult> onFulfilled) {
		messenger.awaitResolution(onFulfilled);

		return new Promise<>(onFulfilled);
	}

	@Override
	public final <TNewResult> Promise<TNewResult> then(FourParameterAction<TResult, IResolvedPromise<TNewResult>, IRejectedPromise, OneParameterAction<Runnable>> onFulfilled) {
		return thenCreateCancellablePromise(new Execution.Cancellable.ErrorPropagatingCancellableExecutor<>(onFulfilled));
	}

	@Override
	public final <TNewResult> Promise<TNewResult> then(CarelessTwoParameterFunction<TResult, OneParameterAction<Runnable>, TNewResult> onFulfilled) {
		return then(new Execution.Cancellable.ExpectedResultCancellableExecutor<>(onFulfilled));
	}

	@Override
	public final <TNewRejectedResult> Promise<TNewRejectedResult> error(FourParameterAction<Throwable, IResolvedPromise<TNewRejectedResult>, IRejectedPromise, OneParameterAction<Runnable>> onRejected) {
		return thenCreateCancellablePromise(new Execution.Cancellable.RejectionDependentCancellableExecutor<>(onRejected));
	}

	@Override
	public final <TNewRejectedResult> Promise<TNewRejectedResult> error(CarelessTwoParameterFunction<Throwable, OneParameterAction<Runnable>, TNewRejectedResult> onRejected) {
		return error(new Execution.Cancellable.ExpectedResultCancellableExecutor<>(onRejected));
	}

	private <TNewResult> Promise<TNewResult> thenCreatePromise(FourParameterAction<TResult, Throwable, IResolvedPromise<TNewResult>, IRejectedPromise> onFulfilled) {
		return
			thenCreateCancellablePromise(new Execution.NonCancellableExecutor<>(onFulfilled));
	}

	@Override
	public final <TNewResult> Promise<TNewResult> thenPromise(CarelessOneParameterFunction<TResult, IPromise<TNewResult>> onFulfilled) {
		return then(new Execution.PromisedResolution<>(onFulfilled));
	}

	@Override
	public final <TNewResult> Promise<TNewResult> then(ThreeParameterAction<TResult, IResolvedPromise<TNewResult>, IRejectedPromise> onFulfilled) {
		return thenCreatePromise(new Execution.ErrorPropagatingResolveExecutor<>(onFulfilled));
	}

	@Override
	public final <TNewResult> Promise<TNewResult> then(final CarelessOneParameterFunction<TResult, TNewResult> onFulfilled) {
		return then(new Execution.ExpectedResultExecutor<>(onFulfilled));
	}

	@Override
	public final <TNewRejectedResult> Promise<TNewRejectedResult> error(ThreeParameterAction<Throwable, IResolvedPromise<TNewRejectedResult>, IRejectedPromise> onRejected) {
		return thenCreatePromise(new Execution.RejectionDependentExecutor<>(onRejected));
	}

	@Override
	public final <TNewRejectedResult> Promise<TNewRejectedResult> error(CarelessOneParameterFunction<Throwable, TNewRejectedResult> onRejected) {
		return error(new Execution.ExpectedResultExecutor<>(onRejected));
	}

	@Override
	public final <TNewResult> Promise<TNewResult> thenPromise(CarelessTwoParameterFunction<TResult, OneParameterAction<Runnable>, IPromise<TNewResult>> onFulfilled) {
		return then(new Execution.Cancellable.ResolvedCancellablePromise<>(onFulfilled));
	}

	public static <TResult> Promise<TResult> empty() {
		return new Promise<>((TResult)null);
	}

	@SafeVarargs
	public static <TResult> IPromise<Collection<TResult>> whenAll(IPromise<TResult>... promises) {
		return whenAll(Arrays.asList(promises));
	}

	public static <TResult> IPromise<Collection<TResult>> whenAll(Collection<IPromise<TResult>> promises) {
		return new Promise<>(new Resolution.AggregatePromiseResolver<>(promises));
	}

	private static class Execution {

		static class InternalCancellablePromiseExecutor<TResult> extends Messenger<Void, TResult> {
			private final ThreeParameterAction<IResolvedPromise<TResult>, IRejectedPromise, OneParameterAction<Runnable>> executor;

			InternalCancellablePromiseExecutor(ThreeParameterAction<IResolvedPromise<TResult>, IRejectedPromise, OneParameterAction<Runnable>> executor) {
				this.executor = executor;
				receiveResolution(null, null);
			}

			@Override
			public void receiveResolution(Void result, Throwable throwable) {
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

		private static class InternalExpectedPromiseExecutor<TResult> implements TwoParameterAction<IResolvedPromise<TResult>, IRejectedPromise> {
			private final CarelessFunction<TResult> executor;

			InternalExpectedPromiseExecutor(CarelessFunction<TResult> executor) {
				this.executor = executor;
			}

			@Override
			public void runWith(IResolvedPromise<TResult> resolve, IRejectedPromise reject) {
				try {
					resolve.withResult(executor.result());
				} catch (Exception e) {
					reject.withError(e);
				}
			}
		}

		private static class PassThroughCallable<TPassThroughResult> implements CarelessFunction<TPassThroughResult> {
			private final TPassThroughResult passThroughResult;

			PassThroughCallable(TPassThroughResult passThroughResult) {
				this.passThroughResult = passThroughResult;
			}

			@Override
			public TPassThroughResult result() throws Exception {
				return passThroughResult;
			}
		}

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
			static class ErrorPropagatingCancellableExecutor<TResult, TNewResult> extends Messenger<TResult, TNewResult> {
				private final FourParameterAction<TResult, IResolvedPromise<TNewResult>, IRejectedPromise, OneParameterAction<Runnable>> onFulfilled;

				ErrorPropagatingCancellableExecutor(FourParameterAction<TResult, IResolvedPromise<TNewResult>, IRejectedPromise, OneParameterAction<Runnable>> onFulfilled) {
					this.onFulfilled = onFulfilled;
				}


				@Override
				public void receiveResolution(TResult result, Throwable exception) {
					if (exception != null) {
						withError(exception);
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
				public void receiveResolution(TResult result, Throwable throwable) {
					if (throwable != null)
						onRejected.runWith(throwable, this, this, this);
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
		static class NonCancellableExecutor<TResult, TNewResult> extends Messenger<TResult, TNewResult> {
			private final FourParameterAction<TResult, Throwable, IResolvedPromise<TNewResult>, IRejectedPromise> onFulfilled;

			NonCancellableExecutor(FourParameterAction<TResult, Throwable, IResolvedPromise<TNewResult>, IRejectedPromise> onFulfilled) {
				this.onFulfilled = onFulfilled;
			}

			@Override
			public void receiveResolution(TResult result, Throwable throwable) {
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

		private static class ResultCollector<TResult> implements CarelessOneParameterFunction<TResult, TResult> {
			private final Collection<TResult> results;

			ResultCollector(Collection<IPromise<TResult>> promises) {
				this.results = new ArrayList<>(promises.size());
				for (IPromise<TResult> promise : promises)
					promise.then(this);
			}

			@Override
			public TResult resultFrom(TResult result) throws Exception {
				results.add(result);
				return result;
			}

			Collection<TResult> getResults() {
				return results;
			}
		}

		private static class CollectedResultsResolver<TResult> extends ResultCollector<TResult> {
			private final int expectedResultSize;
			private IResolvedPromise<Collection<TResult>> resolve;

			CollectedResultsResolver(Collection<IPromise<TResult>> promises) {
				super(promises);

				this.expectedResultSize = promises.size();
			}

			@Override
			public TResult resultFrom(TResult result) throws Exception {
				final TResult resultFrom = super.resultFrom(result);

				attemptResolve();

				return resultFrom;
			}

			CollectedResultsResolver resolveWith(IResolvedPromise<Collection<TResult>> resolve) {
				this.resolve = resolve;

				attemptResolve();

				return this;
			}

			private void attemptResolve() {
				if (resolve == null) return;

				final Collection<TResult> results = getResults();
				if (results.size() < expectedResultSize) return;

				resolve.withResult(results);
			}
		}

		private static class ErrorHandler<TResult> implements CarelessOneParameterFunction<Throwable, Throwable> {

			private IRejectedPromise reject;
			private Throwable error;

			ErrorHandler(Collection<IPromise<TResult>> promises) {
				for (IPromise<TResult> promise : promises) promise.error(this);
			}

			@Override
			public Throwable resultFrom(Throwable throwable) throws Exception {
				this.error = throwable;
				attemptRejection();
				return throwable;
			}

			boolean rejectWith(IRejectedPromise reject) {
				this.reject = reject;

				return attemptRejection();
			}

			private boolean attemptRejection() {
				if (reject != null && error != null) {
					reject.withError(error);
					return true;
				}

				return false;
			}
		}

		private static class AggregatePromiseResolver<TResult> implements ThreeParameterAction<IResolvedPromise<Collection<TResult>>, IRejectedPromise, OneParameterAction<Runnable>>  {

			private final CollectedPromiseCanceller<TResult> canceller;
			private final CollectedResultsResolver<TResult> resolver;
			private final ErrorHandler errorHandler;

			AggregatePromiseResolver(Collection<IPromise<TResult>> promises) {
				resolver = new CollectedResultsResolver<>(promises);
				errorHandler = new ErrorHandler<>(promises);
				canceller = new CollectedPromiseCanceller<>(promises, resolver);
			}

			@Override
			public void runWith(IResolvedPromise<Collection<TResult>> resolve, IRejectedPromise reject, OneParameterAction<Runnable> onCancelled) {
				if (errorHandler.rejectWith(reject)) return;

				resolver.resolveWith(resolve);

				onCancelled.runWith(canceller.rejection(reject));
			}
		}

		private static class CollectedPromiseCanceller<TResult> implements Runnable {

			private IRejectedPromise reject;
			private final Collection<IPromise<TResult>> promises;
			private final ResultCollector<TResult> resultCollector;

			CollectedPromiseCanceller(Collection<IPromise<TResult>> promises, ResultCollector<TResult> resultCollector) {
				this.promises = promises;
				this.resultCollector = resultCollector;
			}

			Runnable rejection(IRejectedPromise reject) {
				this.reject = reject;
				return this;
			}

			@Override
			public void run() {
				for (IPromise<TResult> promise : promises) promise.cancel();

				reject.withError(new AggregateCancellationException(new ArrayList<>(resultCollector.getResults())));
			}
		}
	}
}
