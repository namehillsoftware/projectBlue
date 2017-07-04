package com.lasthopesoftware.messenger.GivenAPromiseThatResolvesInTheFuture;

import com.lasthopesoftware.messenger.promises.Promise;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Created by david on 10/30/16.
 */

public class WhenThePromiseIsCancelledBeforeResolution {
	private static Object result;
	private static Object expectedResult;
	private static Runnable cancellationRunnable;

	@BeforeClass
	public static void before() throws InterruptedException {
		expectedResult = new Object();

		final CountDownLatch latch = new CountDownLatch(1);
		final Promise<Object> promise = new Promise<>((messenger) -> {
			final Thread myNewThread = new Thread(() -> {
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					messenger.sendRejection(e);
					return;
				}
				messenger.sendResolution(expectedResult);
				latch.countDown();
			});

			cancellationRunnable = spy(new ThreadCanceller(myNewThread));
			messenger.cancellationRequested(cancellationRunnable);

			myNewThread.start();
		});

		promise.cancel();

		promise.next(r -> result = r);

		latch.await(1000, TimeUnit.MILLISECONDS);
	}

	@Test
	public void thenTheExpectedResultIsNotPresent() {
		Assert.assertNotEquals(expectedResult, result);
	}

	@Test
	public void thenTheCancellableIsCalled() {
		verify(cancellationRunnable, times(1)).run();
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
