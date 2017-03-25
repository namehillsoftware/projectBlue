package com.lasthopesoftware.promises.GivenAPromiseThatIsCancelled.AndTheRejectionIsPropagatedThroughAResolve;

import com.lasthopesoftware.promises.Promise;

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
		new Promise<String>((resolve, reject, onCancelled) -> onCancelled.runWith(() -> reject.withError(new Exception())))
			.then((result, onCancelled) -> null)
			.error((exception, onCancelled) -> caughtException = exception)
			.cancel();
	}

	@Test
	public void thenTheRejectionIsNull() {
		Assert.assertNull(caughtException);
	}
}
