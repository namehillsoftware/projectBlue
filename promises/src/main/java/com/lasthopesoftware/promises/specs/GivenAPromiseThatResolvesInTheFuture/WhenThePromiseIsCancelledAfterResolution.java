package com.lasthopesoftware.promises.specs.GivenAPromiseThatResolvesInTheFuture;

import com.lasthopesoftware.promises.Promise;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Created by david on 10/30/16.
 */

public class WhenThePromiseIsCancelledAfterResolution {
	private Object result;
	private Object expectedResult;
	private Runnable cancellationRunnable;

	@Before
	public void before() throws InterruptedException {
		expectedResult = new Object();

		final CountDownLatch latch = new CountDownLatch(1);
		final Promise<Object> promise = new Promise<>((resolve, reject, onCancelled) -> {
			final Thread myNewThread = new Thread(() -> {
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				resolve.withResult(expectedResult);
				latch.countDown();
			});

			cancellationRunnable = spy(new ThreadCanceller(myNewThread));
			onCancelled.runWith(cancellationRunnable);

			myNewThread.start();
		});

		promise.then(result -> { this.result = result; });

		latch.await(1000, TimeUnit.MILLISECONDS);

		promise.cancel();
	}

	@Test
	public void thenTheExpectedResultIsPresent() {
		Assert.assertEquals(expectedResult, result);
	}

	@Test
	public void thenTheCancellableIsNotCalled() {
		verify(cancellationRunnable, times(0)).run();
	}

	private static class ThreadCanceller implements Runnable {
		private final Thread myNewThread;

		ThreadCanceller(Thread myNewThread) {
			this.myNewThread = myNewThread;
		}

		@Override
		public void run() {
			myNewThread.interrupt();
		}
	}
}
