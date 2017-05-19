package com.lasthopesoftware.promises;

import com.vedsoft.futures.callables.CarelessOneParameterFunction;
import com.vedsoft.futures.runnables.OneParameterAction;
import com.vedsoft.futures.runnables.ThreeParameterAction;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Created by david on 4/2/17.
 */
final class Resolution {
	static final class ResolveWithPromiseResult<TNewResult> implements CarelessOneParameterFunction<TNewResult, Void> {
		private final IResolvedPromise<TNewResult> resolve;

		ResolveWithPromiseResult(IResolvedPromise<TNewResult> resolve) {
			this.resolve = resolve;
		}

		@Override
		public final Void resultFrom(TNewResult result) {
			resolve.sendResolution(result);
			return null;
		}
	}

	static final class RejectWithPromiseError implements CarelessOneParameterFunction<Throwable, Void> {
		private final IRejectedPromise reject;

		RejectWithPromiseError(IRejectedPromise reject) {
			this.reject = reject;
		}

		@Override
		public final Void resultFrom(Throwable exception) {
			reject.sendRejection(exception);
			return null;
		}
	}

	private static class ResultCollector<TResult> implements CarelessOneParameterFunction<TResult, TResult> {
		private final Collection<TResult> results;

		ResultCollector(Collection<Promise<TResult>> promises) {
			this.results = new ArrayList<>(promises.size());
			for (Promise<TResult> promise : promises)
				promise.next(this);
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

	private static final class CollectedResultsResolver<TResult> extends ResultCollector<TResult> {
		private final int expectedResultSize;
		private IResolvedPromise<Collection<TResult>> resolve;

		CollectedResultsResolver(Collection<Promise<TResult>> promises) {
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

			resolve.sendResolution(results);
		}
	}

	private static final class ErrorHandler<TResult> implements CarelessOneParameterFunction<Throwable, Throwable> {

		private IRejectedPromise reject;
		private Throwable error;

		ErrorHandler(Collection<Promise<TResult>> promises) {
			for (Promise<TResult> promise : promises) promise.error(this);
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
				reject.sendRejection(error);
				return true;
			}

			return false;
		}
	}

	static final class AggregatePromiseResolver<TResult> implements ThreeParameterAction<IResolvedPromise<Collection<TResult>>, IRejectedPromise, OneParameterAction<Runnable>> {

		private final CollectedPromiseCanceller<TResult> canceller;
		private final CollectedResultsResolver<TResult> resolver;
		private final ErrorHandler errorHandler;

		AggregatePromiseResolver(Collection<Promise<TResult>> promises) {
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

	private static final class CollectedPromiseCanceller<TResult> implements Runnable {

		private IRejectedPromise reject;
		private final Collection<Promise<TResult>> promises;
		private final ResultCollector<TResult> resultCollector;

		CollectedPromiseCanceller(Collection<Promise<TResult>> promises, ResultCollector<TResult> resultCollector) {
			this.promises = promises;
			this.resultCollector = resultCollector;
		}

		Runnable rejection(IRejectedPromise reject) {
			this.reject = reject;
			return this;
		}

		@Override
		public void run() {
			for (Promise<TResult> promise : promises) promise.cancel();

			reject.sendRejection(new AggregateCancellationException(new ArrayList<>(resultCollector.getResults())));
		}
	}
}
