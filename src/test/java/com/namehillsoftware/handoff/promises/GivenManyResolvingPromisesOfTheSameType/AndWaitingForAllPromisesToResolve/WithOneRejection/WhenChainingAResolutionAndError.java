package com.namehillsoftware.handoff.promises.GivenManyResolvingPromisesOfTheSameType.AndWaitingForAllPromisesToResolve.WithOneRejection;

import com.namehillsoftware.handoff.promises.Promise;
import com.namehillsoftware.handoff.promises.PromiseTestBase;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by david on 3/18/17.
 */

public class WhenChainingAResolutionAndError extends PromiseTestBase {

	private static final Exception expectedException = new Exception();
	private static Throwable caughtException;
	private static ArrayList<String> result;

	@BeforeClass
	public static void before() {
		final Promise<String> firstPromise = new Promise<>("test_1");
		final Promise<String> secondPromise = new Promise<>("test_2");
		final Promise<String> thirdPromise = new Promise<>("test_3");
		final Promise<String> fourthPromise = new Promise<>((messenger) -> messenger.sendRejection(expectedException));

		Promise.whenAll(firstPromise, secondPromise, thirdPromise, fourthPromise)
			.then(strings -> result = new ArrayList<>(strings))
			.excuse(e -> caughtException = e);
	}

	@Test
	public void thenTheResolutionIsNotCalled() {
		assertThat(result).isNullOrEmpty();
	}

	@Test
	public void thenTheErrorIsCorrect() {
		assertThat(caughtException).isEqualTo(expectedException);
	}
}
