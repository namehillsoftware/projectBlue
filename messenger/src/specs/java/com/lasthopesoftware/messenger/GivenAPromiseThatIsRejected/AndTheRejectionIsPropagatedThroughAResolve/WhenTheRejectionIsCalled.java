package com.lasthopesoftware.messenger.GivenAPromiseThatIsRejected.AndTheRejectionIsPropagatedThroughAResolve;

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
		thrownException = new Exception();
		new Promise<String>((messenger) -> messenger.sendRejection(thrownException))
				.then(result -> null)
				.excuse(exception -> caughtException = exception);
	}

	@Test
	public void thenTheRejectionIsCorrect() {
		Assert.assertEquals(thrownException, caughtException);
	}
}
