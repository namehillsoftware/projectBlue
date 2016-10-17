package com.lasthopesoftware.promises.unfulfilled.specs;

import com.lasthopesoftware.promises.unfulfilled.FulfilledExecutor;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Created by david on 10/12/16.
 */

public class GivenAFulfillingCallable {

	public class WhenExecutingUsingAStandardResolver {

		Object result;

		@Before
		public void before() {
			final FulfilledExecutor<Object, Object> fulfilledExecutor = new FulfilledExecutor<>(innerResult -> this.result = innerResult);
//			fulfilledExecutor.run(new Object(), );
		}

		@Test
		public void thenTheResolverCallsTheCallable() {
			Assert.assertNotNull(result);
		}
	}
}
