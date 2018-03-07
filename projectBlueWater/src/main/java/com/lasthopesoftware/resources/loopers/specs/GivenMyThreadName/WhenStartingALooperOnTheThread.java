package com.lasthopesoftware.resources.loopers.specs.GivenMyThreadName;

import android.os.Looper;

import com.lasthopesoftware.resources.loopers.LooperThreadCreator;
import com.namehillsoftware.handoff.promises.Promise;
import com.namehillsoftware.lazyj.CreateAndHold;
import com.namehillsoftware.lazyj.Lazy;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.concurrent.CountDownLatch;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@RunWith(RobolectricTestRunner.class)
public class WhenStartingALooperOnTheThread {

	private static final CreateAndHold<Promise<Looper>> looperInit = new Lazy<>(() -> LooperThreadCreator.promiseNewLooperThread("MyThreadName"));

	private static CountDownLatch KeepAliveCountdown = new CountDownLatch(1);

	private static Looper looper;

	private static Throwable throwable;

	@BeforeClass
	public static void context() throws Throwable {
		final CountDownLatch countDownLatch = new CountDownLatch(1);
		looperInit.getObject()
			.then(l -> {
				looper = l;
				countDownLatch.countDown();
				return null;
			}, e -> {
				throwable = e;
				return null;
			});

		countDownLatch.await();

		if (throwable != null)
			throw throwable;

		looperInit.getObject()
			.then(l -> {
				KeepAliveCountdown.await();
				return null;
			});
	}

	@Test
	public void thenTheLooperThreadIsCorrect() {
		assertThat(looper.getThread().getName()).isEqualTo("MyThreadName");
	}

	@Test
	public void thenTheLooperThreadIsStarted() {
		assertThat(looper.getThread().isAlive()).isTrue();
	}

	@AfterClass
	public static void teardown() {
		KeepAliveCountdown.countDown();
	}
}
