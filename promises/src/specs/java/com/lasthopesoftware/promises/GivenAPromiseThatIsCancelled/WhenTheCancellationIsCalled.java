package com.lasthopesoftware.promises.GivenAPromiseThatIsCancelled;

import com.lasthopesoftware.promises.Promise;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Created by david on 10/17/16.
 */

public class WhenTheCancellationIsCalled {

	private Throwable thrownException;
	private Throwable caughtException;

	@Before
	public void before() {
		thrownException = new Exception();
		final Promise<String> cancellablePromise = new Promise<>(
			(messenger) -> messenger.cancellationRequested(() -> messenger.sendRejection(thrownException)));

		cancellablePromise.error((exception) -> caughtException = exception);

		cancellablePromise.cancel();
	}

	@Test
	public void thenTheRejectionIsCorrect() {
		Assert.assertEquals(thrownException, caughtException);
	}
}
