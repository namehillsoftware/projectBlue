package com.lasthopesoftware.bluewater.client.playback.file.exoplayer.preparation.GivenUris.AndTheFileTakesALongTimeToPrepare

import android.content.Context
import android.net.Uri
import android.os.Handler
import com.google.android.exoplayer2.LoadControl
import com.google.android.exoplayer2.audio.MediaCodecAudioRenderer
import com.google.android.exoplayer2.source.BaseMediaSource
import com.google.android.exoplayer2.upstream.DefaultAllocator
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.preparation.ConfigureExoPlayerPreparation
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.preparation.ExoPlayerPlaybackPreparer
import com.lasthopesoftware.bluewater.shared.promises.extensions.FuturePromise
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.joda.time.Duration
import org.junit.BeforeClass
import org.junit.Test
import org.mockito.Mockito
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

class WhenPreparing {

	companion object {
		private var exception: Throwable? = null

		@JvmStatic
		@BeforeClass
		fun before() {
			val loadControl = mockk<LoadControl>()
			every { loadControl.allocator } returns DefaultAllocator(true, 1024)
			val preparationConfiguration = mockk<ConfigureExoPlayerPreparation>()
			every { preparationConfiguration.preparationTimeout } returns Duration.standardSeconds(1)

			val preparer = ExoPlayerPlaybackPreparer(
				Mockito.mock(Context::class.java),
				{ Mockito.mock(BaseMediaSource::class.java) },
				loadControl,
				{ Promise<Array<MediaCodecAudioRenderer>> { } }, // never resolve the audio renderers, simulating something that times out
				Mockito.mock(Handler::class.java),
				Mockito.mock(Handler::class.java),
				Mockito.mock(Handler::class.java),
			 	{ Promise(Mockito.mock(Uri::class.java)) },
				preparationConfiguration)

				val promisedPreparedFile = preparer.promisePreparedPlaybackFile(ServiceFile(1),Duration.ZERO)
			try {
				FuturePromise(promisedPreparedFile)[5, TimeUnit.SECONDS]
			} catch (ex: ExecutionException) {
				exception = ex.cause
			}
		}
	}

	@Test
	fun thenAnExceptionIsThrown() {
		assertThat(exception).isInstanceOf(TimeoutException::class.java)
	}
}
