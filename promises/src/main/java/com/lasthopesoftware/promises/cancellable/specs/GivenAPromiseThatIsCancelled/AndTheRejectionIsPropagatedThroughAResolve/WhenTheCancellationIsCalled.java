package com.lasthopesoftware.promises.cancellable.specs.GivenAPromiseThatIsCancelled.AndTheRejectionIsPropagatedThroughAResolve;

import com.lasthopesoftware.promises.CancellablePromise;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Created by david on 10/17/16.
 */

public class WhenTheCancellationIsCalled {

	private Exception caughtException;

	@Before
	public void before() {
		new CancellablePromise<String>((resolve, reject, onCancelled) -> onCancelled.runWith(() -> reject.withError(new Exception())))
			.then((result, onCancelled) -> {})
			.error((exception, onCancelled) -> { caughtException = exception; })
			.cancel();
	}

	@Test
	public void thenTheRejectionIsNull() {
		Assert.assertNull(caughtException);
	}
}
