package com.namehillsoftware.handoff.promises.aggregation;

import com.namehillsoftware.handoff.Messenger;
import com.namehillsoftware.handoff.promises.Promise;
import com.namehillsoftware.handoff.promises.response.ImmediateResponse;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ConcurrentLinkedQueue;

public class CollectedResultsResolver<TResult> implements ImmediateResponse<TResult, TResult> {
	private final Collection<TResult> results;
	private final int expectedResultSize;
	private final Messenger<Collection<TResult>> collectionMessenger;

	public CollectedResultsResolver(Messenger<Collection<TResult>> collectionMessenger, Collection<Promise<TResult>> promises) {
		this.collectionMessenger = collectionMessenger;
		this.results = new ConcurrentLinkedQueue<>();
		for (Promise<TResult> promise : promises)
			promise.then(this);

		this.expectedResultSize = promises.size();

		if (promises.isEmpty())
			collectionMessenger.sendResolution(Collections.emptyList());
	}

	@Override
	public TResult respond(TResult result) {
		results.add(result);

		attemptResolve();

		return result;
	}

	private void attemptResolve() {
		if (collectionMessenger == null) return;

		final Collection<TResult> results = getResults();
		if (results.size() < expectedResultSize) return;

		collectionMessenger.sendResolution(results);
	}

	public final Collection<TResult> getResults() {
		return results;
	}
}
