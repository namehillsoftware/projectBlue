package com.lasthopesoftware.messenger.promise.queued.cancellation.GivenACancellableQueuedPromise;


import com.lasthopesoftware.messenger.promise.queued.QueuedPromise;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

public class WhenThePromiseIsCancelledAndUnnoticed {

	private static Throwable caughtException;
	private static String result;

	@BeforeClass
	public static void before() throws InterruptedException {
		final CountDownLatch promiseLatch = new CountDownLatch(1);
		final QueuedPromise<String> cancellablePromise = new QueuedPromise<>(
			(cancellationToken) -> {
				if (cancellationToken.isCancelled())
					throw new Exception();

				promiseLatch.await();
				return "test";
			}, Executors.newSingleThreadExecutor());

		final CountDownLatch resolveLatch = new CountDownLatch(1);
		cancellablePromise.error((exception) -> {
			caughtException = exception;
			resolveLatch.countDown();
			return null;
		});

		cancellablePromise.next((r) -> {
			result = r;
			resolveLatch.countDown();
			return null;
		});

		promiseLatch.countDown();

		resolveLatch.await();

		cancellablePromise.cancel();
	}

	@Test
	public void thenTheRejectionIsNull() {
		assertThat(caughtException).isNull();
	}

	@Test
	public void thenTheResultIsCorrect() {
		assertThat(result).isEqualTo("test");
	}
}
