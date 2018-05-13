package com.namehillsoftware.handoff.promises;

import com.namehillsoftware.handoff.Messenger;
import com.namehillsoftware.handoff.promises.aggregation.AggregateCancellation;
import com.namehillsoftware.handoff.promises.aggregation.CollectedErrorExcuse;
import com.namehillsoftware.handoff.promises.aggregation.CollectedResultsResolver;
import com.namehillsoftware.handoff.promises.response.ImmediateResponse;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.CancellationException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

final class Resolutions {

	static final class AggregatePromiseResolver<Resolution> implements MessengerOperator<Collection<Resolution>> {

		private final Collection<Promise<Resolution>> promises;

		AggregatePromiseResolver(Collection<Promise<Resolution>> promises) {
			this.promises = promises;
		}

		@Override
		public void send(Messenger<Collection<Resolution>> messenger) {
			if (promises.isEmpty()) {
				messenger.sendResolution(Collections.emptyList());
				return;
			}

			final CollectedErrorExcuse<Resolution> errorHandler = new CollectedErrorExcuse<>(messenger, promises);
			if (errorHandler.isRejected()) return;

			final CollectedResultsResolver<Resolution> resolver = new CollectedResultsResolver<>(messenger, promises);
			messenger.cancellationRequested(new AggregateCancellation<>(messenger, promises, resolver));
		}
	}

	static final class HonorFirstPromise<Resolution> extends Promise<Resolution> implements
		Runnable
	{

		private final Collection<Promise<Resolution>> promises;
		private final ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();
		private final ImmediateResponse<Resolution, Void> promiseProxy = new ImmediateResponse<Resolution, Void>() {
			@Override
			public Void respond(Resolution resolution) {
				resolve(resolution);
				return null;
			}
		};

		private final ImmediateResponse<Throwable, Void> errorProxy = new ImmediateResponse<Throwable, Void>() {
			@Override
			public Void respond(Throwable resolution) {
				final Lock readLock = readWriteLock.readLock();
				readLock.lock();
				try {
					if (isCancelled) return null;
				} finally {
					readLock.unlock();
				}

				reject(resolution);

				return null;
			}
		};

		private boolean isCancelled;

		HonorFirstPromise(Collection<Promise<Resolution>> promises) {
			this.promises = promises;
			for (Promise<Resolution> promise : promises) {
				promise.then(promiseProxy, errorProxy);
			}
			respondToCancellation(this);
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

			for (Promise<Resolution> promise : promises) promise.cancel();
			reject(new CancellationException());
		}
	}
}
