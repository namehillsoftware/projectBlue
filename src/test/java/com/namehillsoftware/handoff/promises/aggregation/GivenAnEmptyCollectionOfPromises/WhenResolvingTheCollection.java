package com.namehillsoftware.handoff.promises.aggregation.GivenAnEmptyCollectionOfPromises;

import com.namehillsoftware.handoff.promises.Promise;
import com.namehillsoftware.handoff.promises.aggregation.CollectedResultsResolver;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Collection;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

public class WhenResolvingTheCollection {

	private static Collection<Void> response;

	@BeforeClass
	public static void before() {
		final Promise<Collection<Void>> messenger = new Promise<>(m -> new CollectedResultsResolver<>(m, Collections.emptyList()));
		messenger.then(v -> response = v);
	}

	@Test
	public void thenTheCollectionResolutionResolvesWithAnEmptyCollection() {
		assertThat(response).isEmpty();
	}
}
