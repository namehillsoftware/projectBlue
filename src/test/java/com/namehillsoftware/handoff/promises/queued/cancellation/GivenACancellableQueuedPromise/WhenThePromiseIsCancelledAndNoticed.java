package com.namehillsoftware.handoff.promises.queued.cancellation.GivenACancellableQueuedPromise;


import com.namehillsoftware.handoff.promises.queued.QueuedPromise;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

public class WhenThePromiseIsCancelledAndNoticed {
	private static Throwable thrownException;
	private static Throwable caughtException;

	@BeforeClass
	public static void before() throws InterruptedException {
		thrownException = new Exception();
		final CountDownLatch promiseBegunLatch = new CountDownLatch(1);
		final CountDownLatch promiseLatch = new CountDownLatch(1);
		final QueuedPromise<String> cancellablePromise = new QueuedPromise<>(
			(cancellationToken) -> {
				promiseBegunLatch.countDown();

				promiseLatch.await();

				if (cancellationToken.isCancelled())
					throw thrownException;

				return "test";
			}, Executors.newSingleThreadExecutor());

		final CountDownLatch rejectionLatch = new CountDownLatch(1);
		cancellablePromise.then(
				(r) -> {
					rejectionLatch.countDown();
					return null;
				},
				(exception) -> {
					caughtException = exception;
					rejectionLatch.countDown();
					return null;
				});

		promiseBegunLatch.await(10, TimeUnit.SECONDS);

		cancellablePromise.cancel();

		promiseLatch.countDown();

		rejectionLatch.await(10, TimeUnit.SECONDS);
	}

	@Test
	public void thenTheRejectionIsCorrect() {
		assertThat(caughtException).isEqualTo(thrownException);
	}
}
