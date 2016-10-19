package com.lasthopesoftware.promises.specs.GivenAPromiseThatResolves;

import com.lasthopesoftware.promises.ExpectedPromise;
import com.vedsoft.futures.callables.OneParameterCallable;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Created by david on 10/17/16.
 */
public class WhenAnotherReturningPromiseIsExpected {

	private String nextReturningPromiseResult;

	@Before
	public void before() {
		final OneParameterCallable<String, String> nextReturningPromise = (result) -> 330 + result;
		new ExpectedPromise<>(() -> "test")
				.then(nextReturningPromise)
				.then(nextResult -> {
					nextReturningPromiseResult = nextResult;
				});
	}

	@Test
	public void thenTheNextActionReturnsAPromiseOfTheCorrectType() {
		Assert.assertEquals("330test", nextReturningPromiseResult);
	}
}
