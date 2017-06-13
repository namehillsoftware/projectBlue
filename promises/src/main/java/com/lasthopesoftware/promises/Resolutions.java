package com.lasthopesoftware.promises;

import com.vedsoft.futures.callables.CarelessOneParameterFunction;

import java.util.ArrayList;
import java.util.Collection;

final class Resolutions {
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

	private static final class SingleResultErrorHandler<TResult> implements CarelessOneParameterFunction<Throwable, Throwable> {

		private final Messenger<TResult> messenger;
		private Throwable error;

		SingleResultErrorHandler(Collection<Promise<TResult>> promises, Messenger<TResult> messenger) {
			this.messenger = messenger;
			for (Promise<TResult> promise : promises) promise.error(this);
		}

		@Override
		public Throwable resultFrom(Throwable throwable) throws Exception {
			messenger.sendRejection(throwable);
			return throwable;
		}
	}

	static final class AggregatePromiseResolver<TResult> extends EmptyMessenger<Collection<TResult>> {

		private final CollectedResultsCanceller<TResult> canceller;
		private final CollectedResultsResolver<TResult> resolver;
		private final ErrorHandler errorHandler;

		AggregatePromiseResolver(Collection<Promise<TResult>> promises) {
			resolver = new CollectedResultsResolver<>(promises);
			errorHandler = new ErrorHandler<>(promises);
			canceller = new CollectedResultsCanceller<>(promises, resolver);
		}

		@Override
		public void requestResolution() {
			if (errorHandler.rejectWith(this)) return;

			resolver.resolveWith(this);

			cancellationRequested(canceller.rejection(this));
		}
	}

	static final class FirstPromiseResolver<Result> extends EmptyMessenger<Result> implements CarelessOneParameterFunction<Result, Result> {

		private final CollectedPromisesCanceller<Result> canceller;
		private final SingleResultErrorHandler<Result> errorHandler;

		FirstPromiseResolver(Collection<Promise<Result>> promises) {
			for (Promise<Result> promise : promises) promise.next(this);

			errorHandler = new SingleResultErrorHandler<>(promises, this);
			canceller = new CollectedPromisesCanceller<>(promises);
		}

		@Override
		public void requestResolution() {
			cancellationRequested(canceller.rejection(this));
		}

		@Override
		public Result resultFrom(Result result) throws Throwable {
			sendResolution(result);

			return result;
		}
	}

	private static final class CollectedResultsCanceller<TResult> implements Runnable {

		private IRejectedPromise reject;
		private final Collection<Promise<TResult>> promises;
		private final ResultCollector<TResult> resultCollector;

		CollectedResultsCanceller(Collection<Promise<TResult>> promises, ResultCollector<TResult> resultCollector) {
			this.promises = promises;
			this.resultCollector = resultCollector;
		}

		Runnable rejection(IRejectedPromise reject) {
			this.reject = reject;
			return this;
		}

		@Override
		public void run() {
			for (Promise<?> promise : promises) promise.cancel();

			reject.sendRejection(new AggregateCancellationException(new ArrayList<>(resultCollector.getResults())));
		}
	}

	private static final class CollectedPromisesCanceller<Result> implements Runnable {

		private Messenger messenger;
		private Collection<Promise<Result>> promises;

		private CollectedPromisesCanceller(Collection<Promise<Result>> promises) {
			this.promises = promises;
		}

		Runnable rejection(Messenger messenger) {
			this.messenger = messenger;
			return this;
		}

		@Override
		public void run() {
			for (Promise<Result> promise : promises) promise.cancel();
		}
	}
}
