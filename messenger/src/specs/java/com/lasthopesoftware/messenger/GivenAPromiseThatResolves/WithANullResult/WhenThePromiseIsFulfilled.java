package com.lasthopesoftware.messenger.GivenAPromiseThatResolves.WithANullResult;

import com.lasthopesoftware.messenger.promises.Promise;

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
				.next(result -> this.result = result);
	}

	@Test
	public void thenTheResultIsNull() {
		Assert.assertNull(result);
	}
}
