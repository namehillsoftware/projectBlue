package com.lasthopesoftware.promises.specs.GivenAPromiseThatIsCancelled.AfterThePromiseIsResolved;

import com.lasthopesoftware.promises.Promise;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.internal.verification.VerificationModeFactory.times;

/**
 * Created by david on 10/17/16.
 */

public class WhenTheCancellationIsCalled {

	private static final Runnable mockCancel = mock(Runnable.class);
	private static final Object expectedResult = new Object();
	private static Object result;

	@BeforeClass
	public static void before() {
		final Promise<Object> cancellablePromise = new Promise<>(
			(resolve, reject, onCancelled) -> {
				onCancelled.runWith(mockCancel);
				resolve.withResult(expectedResult);
			});

		cancellablePromise.then(result -> WhenTheCancellationIsCalled.result = result);

		cancellablePromise.cancel();
	}

	@Test
	public void thenTheCancellationIsNotCalled() {
		verify(mockCancel, times(0)).run();
	}

	@Test
	public void thenTheResultIsCorrect() {
		Assert.assertEquals(expectedResult, result);
	}
}
