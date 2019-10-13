package com.namehillsoftware.handoff.promises.GivenManyResolvingPromisesOfTheSameType.AndWaitingForOnePromiseToResolve.WithOneRejectionEventually;

import com.namehillsoftware.handoff.promises.Promise;
import com.namehillsoftware.handoff.promises.PromiseTestBase;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by david on 3/18/17.
 */

public class WhenChainingAResolutionAndError extends PromiseTestBase {

	private static Throwable caughtException;
	private static String result;

	@BeforeClass
	public static void before() {
		final Promise<String> firstPromise = new Promise<>((messenger) -> {});
		final Promise<String> secondPromise = new Promise<>("test_2");
		final Promise<String> thirdPromise = new Promise<>("test_3");
		final Promise<String> fourthPromise = new Promise<>((messenger) -> messenger.sendRejection(new Exception()));

		Promise.whenAny(firstPromise, secondPromise, thirdPromise, fourthPromise)
			.then(string -> result = string)
			.excuse(e -> caughtException = e);
	}

	@Test
	public void thenTheResolutionIsCalled() {
		assertThat(result).isEqualTo("test_2");
	}

	@Test
	public void thenTheErrorIsNull() {
		assertThat(caughtException).isNull();
	}
}
