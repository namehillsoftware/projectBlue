package com.lasthopesoftware.messenger.promises.GivenAPromiseThatResolvesInTheFuture;

import com.lasthopesoftware.messenger.promises.Promise;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Created by david on 10/20/16.
 */

public class WhenThePromiseIsFulfilled {

	private static Object result;
	private static Object expectedResult;

	@BeforeClass
	public static void before() throws InterruptedException {
		expectedResult = new Object();
		final CountDownLatch latch = new CountDownLatch(1);
		new Promise<>((messenger) -> new Thread(() -> {
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			messenger.sendResolution(expectedResult);
			latch.countDown();
		}).start())
		.then(r -> result = r);

		latch.await(1000, TimeUnit.MILLISECONDS);
	}

	@Test
	public void thenTheExpectedResultIsPresent() {
		Assert.assertEquals(expectedResult, result);
	}
}
