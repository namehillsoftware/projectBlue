package com.lasthopesoftware.resources.loopers.GivenMyThreadName;

import android.os.Handler;
import android.os.Looper;

import com.lasthopesoftware.AndroidContext;
import com.lasthopesoftware.bluewater.shared.promises.extensions.FuturePromise;
import com.lasthopesoftware.resources.loopers.HandlerThreadCreator;

import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.robolectric.Shadows.shadowOf;

public class WhenStartingALooperOnTheThread extends AndroidContext {

	private Looper looper;

	@Override
	public void before() throws InterruptedException, ExecutionException {
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
