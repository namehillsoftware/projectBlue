package com.namehillsoftware.handoff.promises.GivenAPromiseThatIsCancelled;

import com.namehillsoftware.handoff.promises.Promise;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Created by david on 10/17/16.
 */

public class WhenTheCancellationIsAssignedTwice {

	private static Throwable thrownException;
	private static Throwable caughtException;

	@BeforeClass
	public static void before() {
		thrownException = new Exception();
		final Promise<String> cancellablePromise = new Promise<>(
			(messenger) -> {
				messenger.cancellationRequested(() -> messenger.sendRejection(new Exception()));
				messenger.cancellationRequested(() -> messenger.sendRejection(thrownException));
			});

		cancellablePromise.excuse((exception) -> caughtException = exception);

		cancellablePromise.cancel();
	}

	@Test
	public void thenTheRejectionIsTheSecondOne() {
		Assert.assertEquals(thrownException, caughtException);
	}
}
