package com.lasthopesoftware.messenger.GivenAPromiseThatIsCancelled;

import com.lasthopesoftware.messenger.promises.Promise;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Created by david on 10/17/16.
 */

public class WhenTheCancellationIsCalled {

	private static Throwable thrownException;
	private static Throwable caughtException;

	@BeforeClass
	public static void before() {
		thrownException = new Exception();
		final Promise<String> cancellablePromise = new Promise<>(
			(messenger) -> messenger.cancellationRequested(() -> messenger.sendRejection(thrownException)));

		cancellablePromise.excuse((exception) -> caughtException = exception);

		cancellablePromise.cancel();
	}

	@Test
	public void thenTheRejectionIsCorrect() {
		Assert.assertEquals(thrownException, caughtException);
	}
}
