package com.lasthopesoftware.promises;

import com.lasthopesoftware.promises.errors.AggregateCancellationException;
import com.lasthopesoftware.promises.propagation.PromiseProxy;
import com.vedsoft.futures.callables.CarelessOneParameterFunction;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.CancellationException;

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
		private Messenger<Collection<TResult>> collectionMessenger;

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

		CollectedResultsResolver resolveWith(Messenger<Collection<TResult>> collectionMessenger) {
			this.collectionMessenger = collectionMessenger;

			attemptResolve();

			return this;
		}

		private void attemptResolve() {
			if (collectionMessenger == null) return;

			final Collection<TResult> results = getResults();
			if (results.size() < expectedResultSize) return;

			collectionMessenger.sendResolution(results);
		}
	}

	private static final class ErrorHandler<TResult> implements CarelessOneParameterFunction<Throwable, Throwable> {

		private Messenger<Collection<TResult>> messenger;
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

		boolean rejectWith(Messenger<Collection<TResult>> messenger) {
			this.messenger = messenger;

			return attemptRejection();
		}

		private boolean attemptRejection() {
			if (messenger != null && error != null) {
				messenger.sendRejection(error);
				return true;
			}

			return false;
		}
	}

	static final class AggregatePromiseResolver<TResult> extends AwaitingMessenger<Collection<TResult>> {

		AggregatePromiseResolver(Collection<Promise<TResult>> promises) {
			final CollectedResultsResolver<TResult> resolver = new CollectedResultsResolver<>(promises);
			final ErrorHandler<TResult> errorHandler = new ErrorHandler<>(promises);
			final CollectedResultsCanceller<TResult> canceller = new CollectedResultsCanceller<>(promises, resolver);

			if (errorHandler.rejectWith(this)) return;

			resolver.resolveWith(this);

			cancellationRequested(canceller.rejection(this));
		}
	}

	static final class FirstPromiseResolver<Result> extends AwaitingMessenger<Result> implements
		CarelessOneParameterFunction<Result, Result>,
		Runnable {

		private final PromiseProxy<Result> promiseProxy = new PromiseProxy<>(this);

		FirstPromiseResolver(Collection<Promise<Result>> promises) {
			cancellationRequested(this);

			for (Promise<Result> promise : promises) promiseProxy.proxy(promise);
		}

		@Override
		public Result resultFrom(Result result) throws Throwable {
			sendResolution(result);

			return result;
		}

		@Override
		public void run() {
			sendRejection(new CancellationException());
		}
	}

	private static final class CollectedResultsCanceller<TResult> implements Runnable {

		private Messenger<Collection<TResult>> collectionMessenger;
		private final Collection<Promise<TResult>> promises;
		private final ResultCollector<TResult> resultCollector;

		CollectedResultsCanceller(Collection<Promise<TResult>> promises, ResultCollector<TResult> resultCollector) {
			this.promises = promises;
			this.resultCollector = resultCollector;
		}

		Runnable rejection(Messenger<Collection<TResult>> collectionMessenger) {
			this.collectionMessenger = collectionMessenger;
			return this;
		}

		@Override
		public void run() {
			for (Promise<?> promise : promises) promise.cancel();

			collectionMessenger.sendRejection(new AggregateCancellationException(new ArrayList<>(resultCollector.getResults())));
		}
	}
}
