package com.namehillsoftware.handoff.promises.aggregation;

import com.namehillsoftware.handoff.Messenger;
import com.namehillsoftware.handoff.promises.Promise;
import com.namehillsoftware.handoff.promises.response.ImmediateResponse;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class CollectedResultsResolver<TResult> implements ImmediateResponse<TResult, Void> {
	private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
	private final LinkedList<TResult> collectedResults = new LinkedList<>();
	private final int expectedResultSize;
	private final Messenger<Collection<TResult>> collectionMessenger;

	public CollectedResultsResolver(Messenger<Collection<TResult>> collectionMessenger, Collection<Promise<TResult>> promises) {
		this.collectionMessenger = collectionMessenger;

		expectedResultSize = promises.size();

		for (Promise<TResult> promise : promises) promise.then(this);

		if (promises.isEmpty())
			collectionMessenger.sendResolution(Collections.emptyList());
	}

	@Override
	public Void respond(TResult result) {
		final Lock lock = readWriteLock.writeLock();
		lock.lock();
		try {
			collectedResults.add(result);
		} finally {
			lock.unlock();
		}

		attemptResolve();

		return null;
	}

	private void attemptResolve() {
		if (collectionMessenger == null) return;

		final Lock lock = readWriteLock.readLock();
		lock.lock();
		try {
			if (collectedResults.size() < expectedResultSize) return;

			collectionMessenger.sendResolution(collectedResults);
		} finally {
			lock.unlock();
		}
	}

	public final Collection<TResult> getResults() {
		final Lock lock = readWriteLock.readLock();
		lock.lock();
		try {
			return collectedResults;
		} finally {
			lock.unlock();
		}
	}
}
