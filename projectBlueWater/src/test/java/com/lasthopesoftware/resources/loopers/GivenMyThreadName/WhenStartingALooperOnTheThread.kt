package com.lasthopesoftware.resources.loopers.GivenMyThreadName

import android.os.Handler
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.lasthopesoftware.bluewater.shared.promises.extensions.toFuture
import com.lasthopesoftware.resources.loopers.HandlerThreadCreator.promiseNewHandlerThread
import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import java.util.concurrent.CountDownLatch

@RunWith(AndroidJUnit4::class)
class WhenStartingALooperOnTheThread {

	companion object {
		val looper = lazy {
			val looper = promiseNewHandlerThread("MyThreadName", 3).toFuture().get()!!.looper
			val handler = Handler(looper)
			val countDownLatch = CountDownLatch(1)
			handler.post { countDownLatch.countDown() }
			val shadowLooper = shadowOf(looper)
			shadowLooper.isPaused = true
			shadowLooper.idle()
			countDownLatch.await()
			looper
		}
	}

	@Test
	fun thenTheLooperThreadNameIsCorrect() {
		assertThat(looper.value.thread.name).isEqualTo("MyThreadName")
	}

	@Test
	fun thenTheLooperThreadIsStarted() {
		assertThat(looper.value.thread.isAlive).isTrue
	}
}
