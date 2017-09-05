package com.lasthopesoftware.messenger.promises;

import com.lasthopesoftware.messenger.SingleMessageBroadcaster;
import com.lasthopesoftware.messenger.promises.aggregation.AggregateCancellation;
import com.lasthopesoftware.messenger.promises.aggregation.CollectedErrorExcuse;
import com.lasthopesoftware.messenger.promises.aggregation.CollectedResultsResolver;
import com.lasthopesoftware.messenger.promises.propagation.ResolutionProxy;
import com.lasthopesoftware.messenger.promises.response.ImmediateResponse;

import java.util.Collection;
import java.util.concurrent.CancellationException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

final class Resolutions {

	static final class AggregatePromiseResolver<TResult> extends SingleMessageBroadcaster<Collection<TResult>> {

		AggregatePromiseResolver(Collection<Promise<TResult>> promises) {
			final CollectedErrorExcuse<TResult> errorHandler = new CollectedErrorExcuse<>(this, promises);
			if (errorHandler.isRejected()) return;

			final CollectedResultsResolver<TResult> resolver = new CollectedResultsResolver<>(this, promises);
			cancellationRequested(new AggregateCancellation<>(this, promises, resolver));
		}
	}

	static final class FirstPromiseResolver<Result> extends SingleMessageBroadcaster<Result> implements
		Runnable,
		ImmediateResponse<Throwable, Void> {

		private final Collection<Promise<Result>> promises;
		private final ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();

		private boolean isCancelled;

		FirstPromiseResolver(Collection<Promise<Result>> promises) {
			this.promises = promises;
			for (Promise<Result> promise : promises) {
				promise.then(new ResolutionProxy<>(this));
				promise.excuse(this);
			}
			cancellationRequested(this);
		}

		@Override
		public void run() {
			final Lock writeLock = readWriteLock.writeLock();
			writeLock.lock();
			try {
				isCancelled = true;
			} finally {
				writeLock.unlock();
			}

			for (Promise<Result> promise : promises) promise.cancel();
			sendRejection(new CancellationException());
		}

		@Override
		public Void respond(Throwable throwable) throws Throwable {
			final Lock readLock = readWriteLock.readLock();
			readLock.lock();
			try {
				if (isCancelled) return null;
			} finally {
				readLock.unlock();
			}

			sendRejection(throwable);

			return null;
		}
	}
}
