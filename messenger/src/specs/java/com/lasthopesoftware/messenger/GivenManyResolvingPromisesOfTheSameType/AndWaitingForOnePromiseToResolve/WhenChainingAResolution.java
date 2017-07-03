package com.lasthopesoftware.messenger.GivenManyResolvingPromisesOfTheSameType.AndWaitingForOnePromiseToResolve;

import com.lasthopesoftware.messenger.promise.Promise;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by david on 3/18/17.
 */

public class WhenChainingAResolution {
	private static String result;

	@BeforeClass
	public static void before() {
		final Promise<String> firstPromise = new Promise<>("test_1");
		final Promise<String> secondPromise = new Promise<>("test_2");
		final Promise<String> thirdPromise = new Promise<>("test_3");

		Promise.whenAny(firstPromise, secondPromise, thirdPromise)
			.next(string -> result = string);
	}

	@Test
	public void thenTheResolutionIsCorrect() {
		assertThat(result).isEqualTo("test_1");
	}
}
