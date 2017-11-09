package com.namehillsoftware.handoff.promises.GivenAPromiseThatResolves.WithANullResult;

import com.namehillsoftware.handoff.promises.Promise;
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
		Promise.empty()
				.then(result -> this.result = result);
	}

	@Test
	public void thenTheResultIsNull() {
		Assert.assertNull(result);
	}
}
