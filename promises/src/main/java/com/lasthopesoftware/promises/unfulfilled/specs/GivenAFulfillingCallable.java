package com.lasthopesoftware.promises.unfulfilled.specs;

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
		public void BeforeHand() {

		}

		@Test
		public void thenTheResolverCallsTheCallable() {
			Assert.assertNotNull(result);
		}
	}
}
