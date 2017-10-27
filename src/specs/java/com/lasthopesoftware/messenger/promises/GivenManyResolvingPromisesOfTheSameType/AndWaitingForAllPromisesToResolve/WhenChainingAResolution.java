package com.lasthopesoftware.messenger.promises.GivenManyResolvingPromisesOfTheSameType.AndWaitingForAllPromisesToResolve;

import com.lasthopesoftware.messenger.promises.Promise;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by david on 3/18/17.
 */

public class WhenChainingAResolution {
	private static ArrayList<String> result;

	@BeforeClass
	public static void before() {
		final Promise<String> firstPromise = new Promise<>("test_1");
		final Promise<String> secondPromise = new Promise<>("test_2");
		final Promise<String> thirdPromise = new Promise<>("test_3");

		Promise.whenAll(firstPromise, secondPromise, thirdPromise)
			.then(strings -> result = new ArrayList<>(strings));
	}

	@Test
	public void thenTheResolutionIsCorrect() {
		assertThat(result).containsExactly("test_1", "test_2", "test_3");
	}
}
