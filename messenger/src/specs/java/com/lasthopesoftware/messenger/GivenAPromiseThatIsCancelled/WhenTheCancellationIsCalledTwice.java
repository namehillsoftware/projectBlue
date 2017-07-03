package com.lasthopesoftware.messenger.GivenAPromiseThatIsCancelled;

import com.lasthopesoftware.messenger.promise.Promise;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class WhenTheCancellationIsCalledTwice {

	private static final Runnable mockCancel = mock(Runnable.class);

	@BeforeClass
	public static void before() {
		final Promise<String> cancellablePromise =
			new Promise<>((messenger) -> messenger.cancellationRequested(mockCancel));

		cancellablePromise.cancel();
		cancellablePromise.cancel();
	}

	@Test
	public void thenTheCancellationIsCalledOnce() {
		verify(mockCancel, times(1)).run();
	}
}
