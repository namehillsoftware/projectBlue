package com.lasthopesoftware.promises.specs.GivenAPromiseThatResolves.WithANullResult;

import com.lasthopesoftware.promises.ExpectedPromise;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Created by david on 10/20/16.
 */

public class WhenThePromiseIsFulfilled {

	private Object result;

	@Before
	public void before() {
		result = new Object();
		new ExpectedPromise<>(() -> null)
				.then(result -> { this.result = result; });
	}

	@Test
	public void thenTheResultIsNull() {
		Assert.assertNull(result);
	}
}
