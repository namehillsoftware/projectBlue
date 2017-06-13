package com.lasthopesoftware.promises.GivenManyResolvingPromisesOfTheSameType.AndWaitingForOnePromiseToResolve.WithOneRejectionImmediately;

import com.lasthopesoftware.promises.Promise;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by david on 3/18/17.
 */

public class WhenChainingAResolutionAndError {

	private static Throwable caughtException;
	private static String result;
	private static Exception expectedException;

	@BeforeClass
	public static void before() {
		final Promise<String> firstPromise = new Promise<>((messenger) -> {});
		final Promise<String> secondPromise = new Promise<>(() -> "test_2");
		final Promise<String> thirdPromise = new Promise<>(() -> "test_3");
		final Promise<String> rejectingPromise = new Promise<>(() -> {
			expectedException = new Exception();
			throw expectedException;
		});

		Promise.whenAny(rejectingPromise, firstPromise, secondPromise, thirdPromise)
			.next(string -> result = string)
			.error(e -> caughtException = e);
	}

	@Test
	public void thenTheResolutionIsNotCalled() {
		assertThat(result).isNull();
	}

	@Test
	public void thenTheErrorIsCaught() {
		assertThat(caughtException).isEqualTo(expectedException);
	}
}
