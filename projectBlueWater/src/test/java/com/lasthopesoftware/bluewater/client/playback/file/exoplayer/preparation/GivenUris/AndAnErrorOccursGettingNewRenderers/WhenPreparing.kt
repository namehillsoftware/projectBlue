package com.lasthopesoftware.bluewater.client.playback.file.exoplayer.preparation.GivenUris.AndAnErrorOccursGettingNewRenderers

import android.net.Uri
import com.google.android.exoplayer2.LoadControl
import com.google.android.exoplayer2.source.BaseMediaSource
import com.google.android.exoplayer2.upstream.DefaultAllocator
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.preparation.ExoPlayerPlaybackPreparer
import com.lasthopesoftware.bluewater.shared.promises.extensions.toFuture
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.joda.time.Duration
import org.junit.BeforeClass
import org.junit.Test
import org.mockito.Mockito
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit

class WhenPreparing {

	companion object {
		private var exception: Throwable? = null

		@BeforeClass
		@JvmStatic
		fun before() {
			val loadControl = Mockito.mock(LoadControl::class.java)
			Mockito.`when`(loadControl.allocator).thenReturn(DefaultAllocator(true, 1024))
			val preparer = ExoPlayerPlaybackPreparer(
				mockk(relaxed = true),
				{ mockk<BaseMediaSource>() },
				loadControl,
				{ Promise(Exception("Oops")) },
				mockk(),
				mockk(),
				{ Promise(mockk<Uri>()) }
			)

			try {
				preparer.promisePreparedPlaybackFile(ServiceFile(1), Duration.ZERO).toFuture()[1, TimeUnit.SECONDS]
			} catch (ex: ExecutionException) {
				exception = ex.cause
			}
		}
	}

	@Test
	fun thenAnExceptionIsThrown() {
		assertThat(exception?.message).isEqualTo("Oops")
	}
}
