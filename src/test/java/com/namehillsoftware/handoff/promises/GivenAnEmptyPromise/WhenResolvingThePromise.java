package com.namehillsoftware.handoff.promises.GivenAnEmptyPromise;

import com.namehillsoftware.handoff.promises.Promise;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;

import static org.assertj.core.api.Assertions.assertThat;

public class WhenResolvingThePromise {

	private static Object result;

	@BeforeClass
	public static void before() throws InterruptedException {
		final CountDownLatch countDownLatch = new CountDownLatch(1);

		Promise.empty()
			.then(r -> {
				result = r;
				countDownLatch.countDown();
				return null;
			});

		countDownLatch.await();
	}

	@Test
	public void thenTheResultIsEmpty() {
		assertThat(result).isNull();
	}
}
