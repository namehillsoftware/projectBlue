package com.lasthopesoftware.promises.GivenManyResolvingPromisesOfTheSameType;

import com.lasthopesoftware.promises.AggregateCancellationException;
import com.lasthopesoftware.promises.IPromise;
import com.lasthopesoftware.promises.IResolvedPromise;
import com.lasthopesoftware.promises.Promise;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.vedsoft.futures.callables.VoidFunc.runCarelessly;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by david on 3/19/17.
 */

public class WhenCancellingWhileResolving {
	private static AggregateCancellationException aggregateCancellationException;

	@BeforeClass
	public static void before() {
		final List<IResolvedPromise<String>> resolutions = new ArrayList<>();

		final IPromise<String> firstPromise = new Promise<>((resolve, reject) -> resolutions.add(resolve));
		final IPromise<String> secondPromise = new Promise<>((resolve, reject) -> resolutions.add(resolve));
		final IPromise<String> thirdPromise = new Promise<>((resolve, reject) -> resolutions.add(resolve));
		final IPromise<String> fourthPromise = new Promise<>((resolve, reject) -> resolutions.add(resolve));

		final IPromise<Collection<String>> aggregatePromise = Promise.whenAll(firstPromise, secondPromise, thirdPromise, fourthPromise);
		aggregatePromise
			.error(runCarelessly(e -> {
				if (e instanceof AggregateCancellationException)
					aggregateCancellationException = (AggregateCancellationException) e;
			}));

		resolutions.get(0).withResult("resolution_1");
		resolutions.get(1).withResult("resolution_2");
		aggregatePromise.cancel();

		resolutions.get(2).withResult("resolution_3");
		resolutions.get(3).withResult("resolution_4");
	}

	@Test
	public void thenACancellationExceptionOccurs() {
		assertThat(aggregateCancellationException).isNotNull();
	}

	@Test
	public void thenTheCancellationExceptionContainsTheResultsCollectedSoFar() {
		assertThat(aggregateCancellationException.getResults()).containsExactly("resolution_1", "resolution_2");
	}
}
