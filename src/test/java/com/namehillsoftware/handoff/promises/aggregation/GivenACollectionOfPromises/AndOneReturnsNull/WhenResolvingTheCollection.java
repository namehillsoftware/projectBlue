package com.namehillsoftware.handoff.promises.aggregation.GivenACollectionOfPromises.AndOneReturnsNull;

import com.namehillsoftware.handoff.promises.Promise;
import com.namehillsoftware.handoff.promises.aggregation.CollectedResultsResolver;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;

public class WhenResolvingTheCollection {

	private static Collection<Void> response;

	@BeforeClass
	public static void before() {
		final Promise<Collection<Void>> messenger = new Promise<>(m -> new CollectedResultsResolver<>(m, Arrays.asList(Promise.empty(), Promise.empty())));
		messenger.then(v -> response = v);
	}

	@Test
	public void thenTheCollectionResolutionResolvesWithTheNullResults() {
		assertThat(response).containsExactly(null, null);
	}
}
