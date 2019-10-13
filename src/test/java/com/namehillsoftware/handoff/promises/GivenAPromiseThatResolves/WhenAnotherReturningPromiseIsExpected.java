package com.namehillsoftware.handoff.promises.GivenAPromiseThatResolves;

import com.namehillsoftware.handoff.promises.Promise;
import com.namehillsoftware.handoff.promises.PromiseTestBase;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Created by david on 10/17/16.
 */
public class WhenAnotherReturningPromiseIsExpected extends PromiseTestBase {

	private Integer nextReturningPromiseResult;
	private boolean isCalled;

	@Before
	public void before() {
		new Promise<>("test")
				.then(result -> 330 + result.hashCode())
				.then(nextResult -> nextReturningPromiseResult = nextResult)
				.excuse(err -> isCalled = true);
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
