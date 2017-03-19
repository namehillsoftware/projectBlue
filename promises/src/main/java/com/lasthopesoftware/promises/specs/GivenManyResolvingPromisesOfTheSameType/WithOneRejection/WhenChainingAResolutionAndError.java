package com.lasthopesoftware.promises.specs.GivenManyResolvingPromisesOfTheSameType.WithOneRejection;

import com.lasthopesoftware.promises.IPromise;
import com.lasthopesoftware.promises.Promise;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by david on 3/18/17.
 */

public class WhenChainingAResolutionAndError {

	private static final Exception expectedException = new Exception();
	private static Throwable caughtException;
	private static ArrayList<String> result;

	@BeforeClass
	public static void before() {
		final IPromise<String> firstPromise = new Promise<>(() -> "test_1");
		final IPromise<String> secondPromise = new Promise<>(() -> "test_2");
		final IPromise<String> thirdPromise = new Promise<>(() -> "test_3");
		final IPromise<String> fourthPromise = new Promise<>(() -> {
			throw expectedException;
		});

		Promise.whenAll(firstPromise, secondPromise, thirdPromise, fourthPromise)
			.then(strings -> result = new ArrayList<>(strings))
			.error(e -> caughtException = e);
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
