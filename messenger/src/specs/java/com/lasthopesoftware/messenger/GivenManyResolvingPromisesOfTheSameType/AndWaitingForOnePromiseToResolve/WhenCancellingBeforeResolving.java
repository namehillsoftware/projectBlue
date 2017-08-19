package com.lasthopesoftware.messenger.GivenManyResolvingPromisesOfTheSameType.AndWaitingForOnePromiseToResolve;

import com.lasthopesoftware.messenger.Messenger;
import com.lasthopesoftware.messenger.promises.Promise;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CancellationException;

import static org.assertj.core.api.Assertions.assertThat;

public class WhenCancellingBeforeResolving {
	private static CancellationException cancellationException;
	private static String result;
	private static Collection<Throwable> promiseRejections = new ArrayList<>();

	@BeforeClass
	public static void before() {
		final List<Messenger<String>> messengers = new ArrayList<>();

		final Promise<String> firstPromise = new Promise<>(messenger -> {
			messengers.add(messenger);
			messenger.cancellationRequested(() -> messenger.sendRejection(new Exception()));
		});

		firstPromise.excuse(e -> {
			promiseRejections.add(e);
			return null;
		});

		final Promise<String> secondPromise = new Promise<>(messenger -> {
			messengers.add(messenger);
			messenger.cancellationRequested(() -> messenger.sendRejection(new Exception()));
		});

		secondPromise.excuse(e -> {
			promiseRejections.add(e);
			return null;
		});

		final Promise<String> thirdPromise = new Promise<>(messenger -> {
			messengers.add(messenger);
			messenger.cancellationRequested(() -> messenger.sendRejection(new Exception()));
		});

		thirdPromise.excuse(e -> {
			promiseRejections.add(e);
			return null;
		});

		final Promise<String> fourthPromise = new Promise<>(messenger -> {
			messengers.add(messenger);
			messenger.cancellationRequested(() -> messenger.sendRejection(new Exception()));
		});

		fourthPromise.excuse(e -> {
			promiseRejections.add(e);
			return null;
		});

		final Promise<String> racingPromise = Promise.whenAny(firstPromise, secondPromise, thirdPromise, fourthPromise);
		racingPromise
			.then(string -> result = string)
			.excuse(e -> {
				if (e instanceof CancellationException)
					cancellationException = (CancellationException) e;

				return null;
			});

		racingPromise.cancel();
		messengers.get(0).sendResolution("resolution_1");
		messengers.get(1).sendResolution("resolution_2");
		messengers.get(2).sendResolution("resolution_3");
		messengers.get(3).sendResolution("resolution_4");
	}

	@Test
	public void thenACancellationExceptionOccurs() {
		assertThat(cancellationException).isNotNull();
	}

	@Test
	public void thenTheResultIsNotReturned() {
		assertThat(result).isNull();
	}

	@Test
	public void thenFourOtherRejectionsAreCaught() {
		assertThat(promiseRejections).hasSize(4);
	}
}
