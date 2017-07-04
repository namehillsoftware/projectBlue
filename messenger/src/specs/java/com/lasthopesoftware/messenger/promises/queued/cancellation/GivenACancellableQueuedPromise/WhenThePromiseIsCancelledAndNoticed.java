package com.lasthopesoftware.messenger.promises.queued.cancellation.GivenACancellableQueuedPromise;


import com.lasthopesoftware.messenger.promises.queued.QueuedPromise;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

public class WhenThePromiseIsCancelledAndNoticed {
	private static Throwable thrownException;
	private static Throwable caughtException;

	@BeforeClass
	public static void before() throws InterruptedException {
		thrownException = new Exception();
		final CountDownLatch promiseLatch = new CountDownLatch(1);
		final QueuedPromise<String> cancellablePromise = new QueuedPromise<>(
			(cancellationToken) -> {
				promiseLatch.await();

				if (cancellationToken.isCancelled())
					throw thrownException;

				return "test";
			}, Executors.newSingleThreadExecutor());

		final CountDownLatch rejectionLatch = new CountDownLatch(1);
		cancellablePromise.error((exception) -> {
			caughtException = exception;
			rejectionLatch.countDown();
			return null;
		});

		cancellablePromise.cancel();

		promiseLatch.countDown();

		rejectionLatch.await();
	}

	@Test
	public void thenTheRejectionIsCorrect() {
		assertThat(caughtException).isEqualTo(thrownException);
	}
}
