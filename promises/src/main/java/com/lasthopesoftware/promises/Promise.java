package com.lasthopesoftware.promises;

import com.vedsoft.futures.callables.CarelessFunction;
import com.vedsoft.futures.callables.CarelessOneParameterFunction;
import com.vedsoft.futures.runnables.OneParameterAction;
import com.vedsoft.futures.runnables.ThreeParameterAction;
import com.vedsoft.futures.runnables.TwoParameterAction;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

public class Promise<TResult> extends DependentCancellablePromise<Void, TResult> {

	public Promise(ThreeParameterAction<IResolvedPromise<TResult>, IRejectedPromise, OneParameterAction<Runnable>> executor) {
		super(new Execution.InternalCancellablePromiseExecutor<>(executor));
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
				sendInput(null, null);
			}

			@Override
			public void sendInput(Void result, Throwable throwable) {
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
	}

	private static class Resolution {
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
