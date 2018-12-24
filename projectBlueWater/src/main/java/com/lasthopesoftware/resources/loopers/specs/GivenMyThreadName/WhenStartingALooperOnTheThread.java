package com.lasthopesoftware.resources.loopers.specs.GivenMyThreadName;

import android.os.Handler;
import android.os.Looper;
import com.lasthopesoftware.bluewater.shared.promises.extensions.specs.FuturePromise;
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

	@Before
	public void context() throws Throwable {
		looper = new FuturePromise<>(HandlerThreadCreator.promiseNewHandlerThread("MyThreadName", 3)).get().getLooper();

		final Handler handler = new Handler(looper);
		final CountDownLatch countDownLatch1 = new CountDownLatch(1);
		handler.post(countDownLatch1::countDown);

		shadowOf(looper).getScheduler().advanceToLastPostedRunnable();
		countDownLatch1.await();
	}

	@Test
	public void thenTheLooperThreadNameIsCorrect() {
		assertThat(looper.getThread().getName()).isEqualTo("MyThreadName");
	}

	@Test
	public void thenTheLooperThreadIsStarted() {
		assertThat(looper.getThread().isAlive()).isTrue();
	}
}
