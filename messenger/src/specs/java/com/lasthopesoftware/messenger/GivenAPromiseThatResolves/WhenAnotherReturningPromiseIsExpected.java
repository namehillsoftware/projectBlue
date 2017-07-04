package com.lasthopesoftware.messenger.GivenAPromiseThatResolves;

import com.lasthopesoftware.messenger.promises.Promise;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Created by david on 10/17/16.
 */
public class WhenAnotherReturningPromiseIsExpected {

	private Integer nextReturningPromiseResult;
	private boolean isCalled;

	@Before
	public void before() {
		new Promise<>("test")
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
