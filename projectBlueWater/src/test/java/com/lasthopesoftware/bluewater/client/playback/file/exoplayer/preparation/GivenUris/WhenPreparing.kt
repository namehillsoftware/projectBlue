package com.lasthopesoftware.bluewater.client.playback.file.exoplayer.preparation.GivenUris

import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.google.android.exoplayer2.LoadControl
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.SeekParameters
import com.google.android.exoplayer2.Timeline
import com.google.android.exoplayer2.audio.MediaCodecAudioRenderer
import com.google.android.exoplayer2.source.*
import com.google.android.exoplayer2.trackselection.ExoTrackSelection
import com.google.android.exoplayer2.upstream.Allocator
import com.google.android.exoplayer2.upstream.DefaultAllocator
import com.google.android.exoplayer2.upstream.TransferListener
import com.lasthopesoftware.AndroidContext
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.ExoPlayerPlaybackHandler
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.buffering.BufferingExoPlayer
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.preparation.ExoPlayerPlaybackPreparer
import com.lasthopesoftware.bluewater.client.playback.file.preparation.PreparedPlayableFile
import com.lasthopesoftware.bluewater.shared.promises.extensions.FuturePromise
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.joda.time.Duration
import org.junit.Ignore
import org.junit.Test
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@Ignore("Looper doesn't work correctly")
@LooperMode(LooperMode.Mode.PAUSED)
@Config(sdk = [Build.VERSION_CODES.LOLLIPOP_MR1])
class WhenPreparing : AndroidContext() {

	override fun before() {
		val loadControl = mockk<LoadControl>()
		every { loadControl.allocator } returns DefaultAllocator(true, 1024)

		val preparer = ExoPlayerPlaybackPreparer(
			ApplicationProvider.getApplicationContext(),
			{ FakeMediaSource() },
			loadControl,
			{
				val audioRenderer = mockk<MediaCodecAudioRenderer>()
				every { audioRenderer.isReady } returns (true)
				Promise(arrayOf(audioRenderer))
			},
			Handler(Looper.getMainLooper()),
			Handler(Looper.getMainLooper()),
			{ Promise(Uri.EMPTY) }
		)
		val promisedPreparedFile = preparer.promisePreparedPlaybackFile(
			ServiceFile(1),
			Duration.ZERO
		)
		val countDownLatch = CountDownLatch(1)
		promisedPreparedFile
			.then(
				{ countDownLatch.countDown() },
				{ countDownLatch.countDown() })
		val shadowLooper = Shadows.shadowOf(Looper.getMainLooper())
		while (countDownLatch.count > 0) {
			shadowLooper.idleFor(10, TimeUnit.MINUTES)
			Thread.sleep(100)
		}
		preparedFile = FuturePromise(promisedPreparedFile).get()
	}

	@Test
	fun thenAnExoPlayerIsReturned() {
		assertThat(preparedFile!!.playbackHandler).isInstanceOf(
			ExoPlayerPlaybackHandler::class.java
		)
	}

	@Test
	fun thenABufferingFileIsReturned() {
		assertThat(preparedFile!!.bufferingPlaybackFile).isInstanceOf(
			BufferingExoPlayer::class.java
		)
	}

	private class FakeMediaSource : BaseMediaSource() {
		override fun prepareSourceInternal(mediaTransferListener: TransferListener?) {}
		override fun releaseSourceInternal() {}
		override fun getInitialTimeline(): Timeline? {
			return null
		}

		override fun isSingleWindow(): Boolean {
			return false
		}

		override fun getMediaItem(): MediaItem = MediaItem.EMPTY

		override fun maybeThrowSourceInfoRefreshError() {}
		override fun createPeriod(
			id: MediaSource.MediaPeriodId,
			allocator: Allocator,
			startPositionUs: Long
		): MediaPeriod {
			return object : MediaPeriod {
				override fun prepare(callback: MediaPeriod.Callback, positionUs: Long) {
					callback.onPrepared(this)
				}

				override fun maybeThrowPrepareError() {}
				override fun getTrackGroups(): TrackGroupArray {
					return TrackGroupArray()
				}

				override fun selectTracks(
					selections: Array<ExoTrackSelection>,
					mayRetainStreamFlags: BooleanArray,
					streams: Array<SampleStream>,
					streamResetFlags: BooleanArray,
					positionUs: Long
				): Long {
					return 0
				}

				override fun discardBuffer(positionUs: Long, toKeyframe: Boolean) {}
				override fun readDiscontinuity(): Long {
					return 0
				}

				override fun seekToUs(positionUs: Long): Long {
					return 0
				}

				override fun getAdjustedSeekPositionUs(
					positionUs: Long,
					seekParameters: SeekParameters
				): Long {
					return 0
				}

				override fun getBufferedPositionUs(): Long {
					return 0
				}

				override fun getNextLoadPositionUs(): Long {
					return 0
				}

				override fun continueLoading(positionUs: Long): Boolean {
					return false
				}

				override fun isLoading(): Boolean {
					return false
				}

				override fun reevaluateBuffer(positionUs: Long) {}
			}
		}

		override fun releasePeriod(mediaPeriod: MediaPeriod) {}
	}

	companion object {
		private var preparedFile: PreparedPlayableFile? = null
	}
}
