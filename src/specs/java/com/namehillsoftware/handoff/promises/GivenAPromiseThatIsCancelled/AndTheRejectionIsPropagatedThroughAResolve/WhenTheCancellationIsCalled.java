package com.namehillsoftware.handoff.promises.GivenAPromiseThatIsCancelled.AndTheRejectionIsPropagatedThroughAResolve;

import com.namehillsoftware.handoff.promises.Promise;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Created by david on 10/17/16.
 */

public class WhenTheCancellationIsCalled {

	private Throwable caughtException;

	@Before
	public void before() {
		new Promise<String>((messenger) -> messenger.cancellationRequested(() -> messenger.sendRejection(new Exception())))
			.then((result) -> null)
			.excuse((exception) -> caughtException = exception)
			.cancel();
	}

	@Test
	public void thenTheRejectionIsNull() {
		Assert.assertNull(caughtException);
	}
}
