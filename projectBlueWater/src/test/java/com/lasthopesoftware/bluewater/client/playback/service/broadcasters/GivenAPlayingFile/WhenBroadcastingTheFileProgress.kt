package com.lasthopesoftware.bluewater.client.playback.service.broadcasters.GivenAPlayingFile

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.test.core.app.ApplicationProvider
import com.lasthopesoftware.bluewater.client.playback.file.PlayableFile
import com.lasthopesoftware.bluewater.client.playback.file.PlayedFile
import com.lasthopesoftware.bluewater.client.playback.file.PlayingFile
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.TrackPositionBroadcaster
import com.lasthopesoftware.bluewater.shared.promises.extensions.ProgressedPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.ProgressingPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise
import org.assertj.core.api.AssertionsForClassTypes
import org.joda.time.Duration
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
class WhenBroadcastingTheFileProgress {

	companion object {
		private var progress: Long = 0
		private var duration: Long = 0
		private val setupTest = lazy {
			val localBroadcastManager = LocalBroadcastManager.getInstance(ApplicationProvider.getApplicationContext())
			val countDownLatch = CountDownLatch(1)
			localBroadcastManager.registerReceiver(object : BroadcastReceiver() {
				override fun onReceive(context: Context, intent: Intent) {
					duration = intent.getLongExtra(TrackPositionBroadcaster.TrackPositionChangedParameters.fileDuration, -1)
					progress = intent.getLongExtra(TrackPositionBroadcaster.TrackPositionChangedParameters.filePosition, -1)
					countDownLatch.countDown()
				}
			}, IntentFilter(TrackPositionBroadcaster.trackPositionUpdate))
			val trackPositionBroadcaster = TrackPositionBroadcaster(
				localBroadcastManager,
				object : PlayingFile {
					override fun promisePause(): Promise<PlayableFile> {
						return Promise.empty()
					}

					override fun promisePlayedFile(): ProgressedPromise<Duration, PlayedFile> {
						return object : ProgressingPromise<Duration, PlayedFile>() {
							override val progress: Promise<Duration>
								get() = Duration.ZERO.toPromise()
						}
					}

					override val duration: Promise<Duration>
						get() = Duration.standardMinutes(3).toPromise()
				})
			trackPositionBroadcaster.accept(
				Duration
					.standardSeconds(2)
					.plus(Duration.standardSeconds(30)))
			countDownLatch.await(1, TimeUnit.SECONDS)
		}
	}

	@Before
	fun before() {
		setupTest.value
	}

	@Test
	fun thenTheProgressIsCorrect() {
		AssertionsForClassTypes.assertThat(progress).isEqualTo(Duration
			.standardSeconds(2)
			.plus(Duration.standardSeconds(30)).millis)
	}

	@Test
	fun thenTheDurationIsCorrect() {
		AssertionsForClassTypes.assertThat(duration).isEqualTo(Duration.standardMinutes(3).millis)
	}
}
