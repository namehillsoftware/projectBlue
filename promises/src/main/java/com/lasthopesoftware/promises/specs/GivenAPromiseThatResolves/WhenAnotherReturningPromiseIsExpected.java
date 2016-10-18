package com.lasthopesoftware.promises.specs.GivenAPromiseThatResolves;

import com.lasthopesoftware.promises.Promise;
import com.vedsoft.futures.callables.OneParameterCallable;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Created by david on 10/17/16.
 */
public class WhenAnotherReturningPromiseIsExpected {

	private int nextReturningPromiseResult;

	@Before
	public void before() {
		final OneParameterCallable<String, Integer> nextReturningPromise = (result) -> 330;
		new Promise<String>((resolve, reject) -> resolve.run("test"))
				.then(nextReturningPromise)
				.then(nextResult -> {
					nextReturningPromiseResult = nextResult;
				});
	}

	@Test
	public void thenTheNextActionReturnsAPromiseOfTheCorrectType() {
		Assert.assertEquals(330, nextReturningPromiseResult);
	}
}
