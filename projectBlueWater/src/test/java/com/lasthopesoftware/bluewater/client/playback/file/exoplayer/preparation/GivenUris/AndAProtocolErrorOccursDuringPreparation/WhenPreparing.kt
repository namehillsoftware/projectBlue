package com.lasthopesoftware.bluewater.client.playback.file.exoplayer.preparation.GivenUris.AndAProtocolErrorOccursDuringPreparation

import android.content.Context
import android.net.Uri
import android.os.Handler
import androidx.test.core.app.ApplicationProvider
import com.google.android.exoplayer2.LoadControl
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.audio.MediaCodecAudioRenderer
import com.google.android.exoplayer2.source.BaseMediaSource
import com.google.android.exoplayer2.upstream.DataSpec
import com.google.android.exoplayer2.upstream.DefaultAllocator
import com.google.android.exoplayer2.upstream.HttpDataSource
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.playback.file.EmptyPlaybackHandler
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.preparation.ExoPlayerPlaybackPreparer
import com.lasthopesoftware.bluewater.shared.cls
import com.lasthopesoftware.bluewater.shared.promises.extensions.toFuture
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.joda.time.Duration
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import java.net.ProtocolException

@RunWith(RobolectricTestRunner::class)
class WhenPreparing {

	companion object {
		private val preparedPlayer by lazy {
			val loadControl = mockk<LoadControl>(relaxed = true).apply {
				every { allocator } returns DefaultAllocator(true, 1024)
				every { backBufferDurationUs } returns 448
			}

			val application = ApplicationProvider.getApplicationContext<Context>()
			val preparer = ExoPlayerPlaybackPreparer(
				application,
				{
					mockk<BaseMediaSource>(relaxUnitFun = true).apply {
						every { isSingleWindow } returns true
						every { initialTimeline } returns null
						every { mediaItem } returns mockk()
						every { prepareSource(any(), any(), any()) } throws HttpDataSource.HttpDataSourceException(
							ProtocolException("http://fool/"),
							DataSpec(mockk()),
							PlaybackException.ERROR_CODE_IO_UNSPECIFIED,
							HttpDataSource.HttpDataSourceException.TYPE_OPEN
						)
					}
				},
				loadControl,
				{
					val audioRenderer = mockk<MediaCodecAudioRenderer>(relaxUnitFun = true, relaxed = true)
					every { audioRenderer.isReady } returns (true)
					Promise(arrayOf(audioRenderer))
				},
				Handler(application.mainLooper),
				Handler(application.mainLooper),
				{ Promise(mockk<Uri>()) }
			)

			val future = preparer.promisePreparedPlaybackFile(ServiceFile(1), Duration.ZERO).toFuture()

			val shadowLooper = Shadows.shadowOf(application.mainLooper)
			shadowLooper.idle()

			future.get()
		}
	}

	@Test
	fun thenAnEmptyPlayerIsReturned() {
		assertThat(preparedPlayer).isInstanceOf(cls<EmptyPlaybackHandler>())
	}
}
