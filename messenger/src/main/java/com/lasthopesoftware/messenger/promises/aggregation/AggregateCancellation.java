package com.lasthopesoftware.messenger.promises.aggregation;

import com.lasthopesoftware.messenger.Messenger;
import com.lasthopesoftware.messenger.errors.AggregateCancellationException;
import com.lasthopesoftware.messenger.promises.Promise;

import java.util.ArrayList;
import java.util.Collection;

public class AggregateCancellation<TResult> implements Runnable {

	private final Messenger<Collection<TResult>> collectionMessenger;
	private final Collection<Promise<TResult>> promises;
	private final CollectedResultsResolver<TResult> resultCollector;

	public AggregateCancellation(Messenger<Collection<TResult>> messenger, Collection<Promise<TResult>> promises, CollectedResultsResolver<TResult> resultCollector) {
		this.collectionMessenger = messenger;
		this.promises = promises;
		this.resultCollector = resultCollector;
	}

	@Override
	public void run() {
		for (Promise<?> promise : promises) promise.cancel();

		collectionMessenger.sendRejection(new AggregateCancellationException(new ArrayList<>(resultCollector.getResults())));
	}
}
