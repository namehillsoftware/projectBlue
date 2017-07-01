package com.lasthopesoftware.promises.queued.cancellation.GivenACancellableQueuedPromise;


import com.lasthopesoftware.promises.queued.QueuedPromise;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

public class WhenThePromiseIsCancelled {
	private static Throwable thrownException;
	private static Throwable caughtException;

	@BeforeClass
	public static void before() {
		thrownException = new Exception();
		final CountDownLatch countDownLatch = new CountDownLatch(1);
		final QueuedPromise<String> cancellablePromise = new QueuedPromise<>(
			(cancellationToken) -> {
				countDownLatch.await();

				if (cancellationToken.isCancelled())
					throw thrownException;

				return "test";
			}, Executors.newSingleThreadExecutor());

		cancellablePromise.error((exception) -> caughtException = exception);

		cancellablePromise.cancel();

		countDownLatch.countDown();
	}

	@Test
	public void thenTheRejectionIsCorrect() {
		assertThat(caughtException).isEqualTo(thrownException);
	}
}
