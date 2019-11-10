package com.namehillsoftware.handoff.promises.GivenAPromiseThatResolves.AndInevitablyFinishesWithAnAction;

import com.namehillsoftware.handoff.promises.Promise;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Created by david on 10/17/16.
 */
public class WhenThePromiseResolves {

	private String nextReturningPromiseResult;
	private boolean isCalled;

	@Before
	public void before() {
		new Promise<>("test")
				.inevitably(() -> new Promise<>(isCalled = true))
				.then(r -> nextReturningPromiseResult = r);
	}

	@Test
	public void thenTheNextActionReturnsAPromiseOfTheCorrectType() {
		Assert.assertEquals("test", nextReturningPromiseResult);
	}

	@Test
	public void thenTheAlwaysConditionIsCalled() {
		Assert.assertTrue(isCalled);
	}
}
