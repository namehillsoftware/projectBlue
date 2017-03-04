package com.lasthopesoftware.promises.specs.GivenAPromiseThatIsRejected;

import com.lasthopesoftware.promises.Promise;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Created by david on 10/17/16.
 */

public class WhenTheRejectionIsCalled {

	private Exception thrownException;
	private Throwable caughtException;

	@Before
	public void before() {
		thrownException = new Exception();
		new Promise<String>(
				() -> {
					throw thrownException;
				}).error(exception -> caughtException = exception);
	}

	@Test
	public void thenTheRejectionIsCorrect() {
		Assert.assertEquals(thrownException, caughtException);
	}
}
