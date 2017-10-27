package com.lasthopesoftware.messenger.promises.GivenAPromiseThatIsRejected.AndTheRejectionIsPropagatedThroughAResolve;

import com.lasthopesoftware.messenger.promises.Promise;

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
		new Promise<String>(thrownException = new Exception())
				.then(result -> null)
				.excuse(exception -> caughtException = exception);
	}

	@Test
	public void thenTheRejectionIsCorrect() {
		Assert.assertEquals(thrownException, caughtException);
	}
}
