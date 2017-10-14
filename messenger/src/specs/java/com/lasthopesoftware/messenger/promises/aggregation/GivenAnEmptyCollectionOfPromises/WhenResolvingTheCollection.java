package com.lasthopesoftware.messenger.promises.aggregation.GivenAnEmptyCollectionOfPromises;

import com.lasthopesoftware.messenger.Messenger;
import com.lasthopesoftware.messenger.SingleMessageBroadcaster;
import com.lasthopesoftware.messenger.promises.aggregation.CollectedResultsResolver;

import org.junit.BeforeClass;

import java.util.Collections;

public class WhenResolvingTheCollection {

	@BeforeClass
	public static void before() {
		final Messenger<Void> messenger = new SingleMessageBroadcaster<>();

		final CollectedResultsResolver<Void> collectedResultsResolver =
			new CollectedResultsResolver<Void>(new SingleMessageBroadcaster<>(), Collections.emptyList());
	}

	public void thenTheCollectionResolutionResolvesWithAnEmptyCollection() {

	}
}
