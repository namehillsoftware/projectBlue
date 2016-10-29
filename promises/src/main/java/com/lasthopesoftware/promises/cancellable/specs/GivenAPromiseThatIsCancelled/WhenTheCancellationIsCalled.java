package com.lasthopesoftware.promises.cancellable.specs.GivenAPromiseThatIsCancelled;

import com.lasthopesoftware.promises.cancellable.CancellablePromise;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Created by david on 10/17/16.
 */

public class WhenTheCancellationIsCalled {

	private Exception thrownException;
	private Exception caughtException;

	@Before
	public void before() {
		thrownException = new Exception();
		new CancellablePromise<String>(
			(resolve, reject, onCancelled) -> onCancelled.runWith(() -> reject.withError(thrownException)))
			.error((exception, onCancelled) -> {
				caughtException = exception;
			}).cancel();
	}

	@Test
	public void thenTheRejectionIsCorrect() {
		Assert.assertEquals(thrownException, caughtException);
	}
}
