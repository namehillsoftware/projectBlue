package com.namehillsoftware.handoff.promises.aggregation;

import com.namehillsoftware.handoff.Messenger;
import com.namehillsoftware.handoff.promises.Promise;
import com.namehillsoftware.handoff.promises.response.ImmediateResponse;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ConcurrentLinkedQueue;

public class CollectedResultsResolver<TResult> implements ImmediateResponse<TResult, TResult> {
	private final Collection<TResult> collectedResults = new ConcurrentLinkedQueue<>();
	private final ArrayList<TResult> finalResults;
	private final int expectedResultSize;
	private final Messenger<Collection<TResult>> collectionMessenger;

	public CollectedResultsResolver(Messenger<Collection<TResult>> collectionMessenger, Collection<Promise<TResult>> promises) {
		this.collectionMessenger = collectionMessenger;
		this.finalResults = new ArrayList<>(promises.size());
		for (Promise<TResult> promise : promises)
			promise.then(this);

		this.expectedResultSize = promises.size();

		if (promises.isEmpty())
			collectionMessenger.sendResolution(Collections.emptyList());
	}

	@Override
	public TResult respond(TResult result) {
		collectedResults.add(result);

		attemptResolve();

		return result;
	}

	private void attemptResolve() {
		if (collectionMessenger == null) return;

		final Collection<TResult> results = getCollectedResults();
		if (results.size() < expectedResultSize) return;

		this.finalResults.addAll(results);

		collectionMessenger.sendResolution(results);
	}

	public final Collection<TResult> getCollectedResults() {
		return collectedResults;
	}
}
