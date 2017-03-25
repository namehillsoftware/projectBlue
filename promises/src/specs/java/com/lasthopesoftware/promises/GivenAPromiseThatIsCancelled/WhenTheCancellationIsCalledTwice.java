package com.lasthopesoftware.promises.GivenAPromiseThatIsCancelled;

import com.lasthopesoftware.promises.Promise;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Created by david on 10/17/16.
 */

public class WhenTheCancellationIsCalledTwice {

	private static final Runnable mockCancel = mock(Runnable.class);

	@BeforeClass
	public static void before() {
		final Promise<String> cancellablePromise =
			new Promise<>((resolve, reject, onCancelled) -> onCancelled.runWith(mockCancel));

		cancellablePromise.cancel();
		cancellablePromise.cancel();
	}

	@Test
	public void thenTheCancellationIsCalledOnce() {
		verify(mockCancel, times(1)).run();
	}
}
