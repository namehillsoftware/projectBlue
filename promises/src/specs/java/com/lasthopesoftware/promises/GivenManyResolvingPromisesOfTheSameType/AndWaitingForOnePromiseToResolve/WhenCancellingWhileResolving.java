package com.lasthopesoftware.promises.GivenManyResolvingPromisesOfTheSameType.AndWaitingForOnePromiseToResolve;

import com.lasthopesoftware.promises.AggregateCancellationException;
import com.lasthopesoftware.promises.Messenger;
import com.lasthopesoftware.promises.Promise;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
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
		final List<Messenger<String>> messengers = new ArrayList<>();

		final Promise<String> firstPromise = new Promise<>(messengers::add);
		final Promise<String> secondPromise = new Promise<>(messengers::add);
		final Promise<String> thirdPromise = new Promise<>(messengers::add);
		final Promise<String> fourthPromise = new Promise<>(messengers::add);

		final Promise<String> racingPromise = Promise.whenAny(firstPromise, secondPromise, thirdPromise, fourthPromise);
		racingPromise
			.error(runCarelessly(e -> {
				if (e instanceof AggregateCancellationException)
					aggregateCancellationException = (AggregateCancellationException) e;
			}));

		messengers.get(0).sendResolution("resolution_1");
		messengers.get(1).sendResolution("resolution_2");
		racingPromise.cancel();

		messengers.get(2).sendResolution("resolution_3");
		messengers.get(3).sendResolution("resolution_4");
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
