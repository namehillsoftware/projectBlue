package com.lasthopesoftware.messenger.GivenACancellablePromiseThatResolves;

import com.lasthopesoftware.messenger.promises.Promise;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Created by david on 10/17/16.
 */
public class WhenAnotherReturningPromiseIsExpected {

	private static Integer nextReturningPromiseResult;
	private static boolean isCalled;

	@BeforeClass
	public static void before() {
		new Promise<>((messenger) -> messenger.sendResolution("test"))
				.next(result -> 330 + result.hashCode())
				.next(nextResult -> nextReturningPromiseResult = nextResult)
				.error(err -> isCalled = true);
	}

	@Test
	public void thenTheNextActionReturnsAPromiseOfTheCorrectType() {
		Assert.assertEquals(330 + "test".hashCode(), nextReturningPromiseResult.intValue());
	}

	@Test
	public void thenTheErrorIsNotCalled() {
		Assert.assertFalse(isCalled);
	}
}
