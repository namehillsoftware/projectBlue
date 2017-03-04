package com.lasthopesoftware.promises.specs.GivenAPromiseThatIsCancelled;

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
			(resolve, reject, onCancelled) -> onCancelled.runWith(() -> reject.withError(thrownException)));

		cancellablePromise.error((exception, onCancelled) -> caughtException = exception);

		cancellablePromise.cancel();
	}

	@Test
	public void thenTheRejectionIsCorrect() {
		Assert.assertEquals(thrownException, caughtException);
	}
}
