package com.namehillsoftware.handoff.promises.GivenAPromiseThatResolvesInTheFuture;

import com.namehillsoftware.handoff.promises.Promise;
import com.namehillsoftware.handoff.promises.PromiseTestBase;
import com.namehillsoftware.handoff.promises.response.ImmediateResponse;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.mockito.Mockito.*;

public class WhenChainingResolutionTwice extends PromiseTestBase {

	private static ImmediateResponse<String, ?> firstResultHandler;
	private static ImmediateResponse<String, ?> secondResultHandler;

	@SuppressWarnings("unchecked")
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

		firstResultHandler = mock(ImmediateResponse.class);

		rootPromise
			.then(firstResultHandler);

		secondResultHandler = mock(ImmediateResponse.class);

		rootPromise
			.then(secondResultHandler);

		latch.await(1000, TimeUnit.MILLISECONDS);
	}

	@Test
	public void thenTheFirstResolutionIsCalled() throws Throwable {
		verify(firstResultHandler, times(1)).respond(any());
	}

	@Test
	public void thenTheSecondResolutionIsCalled() throws Throwable {
		verify(secondResultHandler, times(1)).respond(any());
	}
}
