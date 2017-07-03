package com.lasthopesoftware.messenger.GivenAPromiseThatResolvesInTheFuture;

import com.lasthopesoftware.messenger.promise.Promise;
import com.vedsoft.futures.callables.CarelessOneParameterFunction;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Created by david on 10/29/16.
 */

public class WhenChainingResolutionTwice {

	private static CarelessOneParameterFunction<String, ?> firstResultHandler;
	private static CarelessOneParameterFunction<String, ?> secondResultHandler;

	@BeforeClass
	public static void before() throws InterruptedException {
		final CountDownLatch latch = new CountDownLatch(1);
		final Promise<String> rootPromise =
			new Promise<>((messenger) -> new Thread(() -> {
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				messenger.sendResolution("test");
				latch.countDown();
			}).start());

		firstResultHandler = mock(CarelessOneParameterFunction.class);

		rootPromise
			.next(firstResultHandler);

		secondResultHandler = mock(CarelessOneParameterFunction.class);

		rootPromise
			.next(secondResultHandler);

		latch.await(1000, TimeUnit.MILLISECONDS);
	}

	@Test
	public void thenTheFirstResolutionIsCalled() throws Throwable {
		verify(firstResultHandler, times(1)).resultFrom(any());
	}

	@Test
	public void thenTheSecondResolutionIsCalled() throws Throwable {
		verify(secondResultHandler, times(1)).resultFrom(any());
	}
}
