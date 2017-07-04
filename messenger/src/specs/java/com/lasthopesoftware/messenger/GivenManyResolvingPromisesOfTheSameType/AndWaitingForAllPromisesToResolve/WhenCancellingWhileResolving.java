package com.lasthopesoftware.messenger.GivenManyResolvingPromisesOfTheSameType.AndWaitingForAllPromisesToResolve;

import com.lasthopesoftware.messenger.Messenger;
import com.lasthopesoftware.messenger.errors.AggregateCancellationException;
import com.lasthopesoftware.messenger.promises.Promise;

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
		final List<Messenger<String>> resolutions = new ArrayList<>();

		final Promise<String> firstPromise = new Promise<>(resolutions::add);
		final Promise<String> secondPromise = new Promise<>(resolutions::add);
		final Promise<String> thirdPromise = new Promise<>(resolutions::add);
		final Promise<String> fourthPromise = new Promise<>(resolutions::add);

		final Promise<Collection<String>> aggregatePromise = Promise.whenAll(firstPromise, secondPromise, thirdPromise, fourthPromise);
		aggregatePromise
			.error(runCarelessly(e -> {
				if (e instanceof AggregateCancellationException)
					aggregateCancellationException = (AggregateCancellationException) e;
			}));

		resolutions.get(0).sendResolution("resolution_1");
		resolutions.get(1).sendResolution("resolution_2");
		aggregatePromise.cancel();

		resolutions.get(2).sendResolution("resolution_3");
		resolutions.get(3).sendResolution("resolution_4");
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
