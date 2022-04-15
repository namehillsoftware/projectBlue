package com.lasthopesoftware.bluewater.client.browsing.items.media.audio.uri.GivenTheUriIsNotAvailable

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.cached.access.ICachedFilesProvider
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.cached.repository.CachedFile
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.uri.RemoteFileUriProvider
import com.lasthopesoftware.bluewater.client.playback.caching.uri.CachedAudioFileUriProvider
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.AssertionsForInterfaceTypes.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WhenProvidingTheUri {

	companion object {
		private val cachedFileUri by lazy {
			val remoteFileUriProvider = mockk<RemoteFileUriProvider>().apply {
				every { promiseFileUri(ServiceFile(10)) } returns Promise.empty()
			}

			val cachedFilesProvider = mockk<ICachedFilesProvider>().apply {
				every { promiseCachedFile("file?key=1") } returns Promise(CachedFile())
			}

			val cachedAudioFileUriProvider = CachedAudioFileUriProvider(
				remoteFileUriProvider,
				cachedFilesProvider
			)

			cachedAudioFileUriProvider
				.promiseFileUri(ServiceFile(10))
				.toExpiringFuture()
				.get()
		}
	}

	@Test
	fun thenTheUriIsEmpty() {
		assertThat(cachedFileUri).isNull()
	}
}
