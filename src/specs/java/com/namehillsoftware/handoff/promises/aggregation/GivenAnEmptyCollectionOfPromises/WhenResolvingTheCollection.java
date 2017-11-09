package com.namehillsoftware.handoff.promises.aggregation.GivenAnEmptyCollectionOfPromises;

import com.namehillsoftware.handoff.Message;
import com.namehillsoftware.handoff.SingleMessageBroadcaster;
import com.namehillsoftware.handoff.promises.aggregation.CollectedResultsResolver;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Collection;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

public class WhenResolvingTheCollection {

	private static Message<Collection<Void>> response;

	@BeforeClass
	public static void before() {
		final SingleMessageBroadcaster<Collection<Void>> messenger = new SingleMessageBroadcaster<>();
		messenger.awaitResolution(v -> response = v);

		new CollectedResultsResolver<>(messenger, Collections.emptyList());
	}

	@Test
	public void thenTheCollectionResolutionResolvesWithAnEmptyCollection() {
		assertThat(response.resolution).isEmpty();
	}
}
