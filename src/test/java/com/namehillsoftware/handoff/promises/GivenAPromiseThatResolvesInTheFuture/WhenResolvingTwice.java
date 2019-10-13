package com.namehillsoftware.handoff.promises.GivenAPromiseThatResolvesInTheFuture;

import com.namehillsoftware.handoff.promises.Promise;
import com.namehillsoftware.handoff.promises.PromiseTestBase;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Created by david on 2/20/17.
 */

public class WhenResolvingTwice extends PromiseTestBase {

	private static Object expectedResult = new Object();
	private static Object unexpectedResult = new Object();
	private static Object result;

	@BeforeClass
	public static void before() throws InterruptedException {
		final CountDownLatch latch = new CountDownLatch(2);

		new Promise<>((messenger) ->  new Thread(() -> {
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			messenger.sendResolution(expectedResult);
			latch.countDown();
			messenger.sendResolution(unexpectedResult);
			latch.countDown();
		}).start())
		.then(result -> WhenResolvingTwice.result = result);

		latch.await(1000, TimeUnit.MILLISECONDS);
	}

	@Test
	public void thenTheExpectedResultIsPresent() {
		Assert.assertEquals(expectedResult, result);
	}
}
