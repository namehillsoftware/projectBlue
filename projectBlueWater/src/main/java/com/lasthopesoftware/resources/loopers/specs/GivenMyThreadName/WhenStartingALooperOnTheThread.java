package com.lasthopesoftware.resources.loopers.specs.GivenMyThreadName;

import android.os.Handler;
import android.os.Looper;

import com.lasthopesoftware.resources.loopers.HandlerThreadCreator;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.concurrent.CountDownLatch;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
public class WhenStartingALooperOnTheThread {

	private Looper looper;

	private Throwable throwable;

	@Before
	public void context() throws Throwable {
		final CountDownLatch countDownLatch = new CountDownLatch(1);
		HandlerThreadCreator.promiseNewHandlerThread("MyThreadName")
			.then(h -> {
				looper = h.getLooper();
				countDownLatch.countDown();
				return null;
			}, e -> {
				throwable = e;
				return null;
			});

		countDownLatch.await();

		if (throwable != null)
			throw throwable;

		final Handler handler = new Handler(looper);
		final CountDownLatch countDownLatch1 = new CountDownLatch(1);
		handler.post(countDownLatch1::countDown);

		shadowOf(looper).getScheduler().advanceToLastPostedRunnable();
		countDownLatch1.await();
	}

	@Test
	public void thenTheLooperThreadIsCorrect() {
		assertThat(looper.getThread().getName()).isEqualTo("MyThreadName");
	}

	@Test
	public void thenTheLooperThreadIsStarted() {
		assertThat(looper.getThread().isAlive()).isTrue();
	}
}
