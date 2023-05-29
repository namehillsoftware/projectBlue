package com.lasthopesoftware.bluewater.client.browsing.items.media.audio.uri.GivenTheUriIsNotAvailable

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.cached.CacheFiles
import com.lasthopesoftware.bluewater.client.browsing.files.uri.RemoteFileUriProvider
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.playback.caching.uri.CachedAudioFileUriProvider
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.AssertionsForInterfaceTypes.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

private const val libraryId = 297

@RunWith(RobolectricTestRunner::class)
class WhenProvidingTheUri {

	companion object {
		private val cachedFileUri by lazy {
			val remoteFileUriProvider = mockk<RemoteFileUriProvider> {
				every { promiseUri(LibraryId(libraryId), ServiceFile(10)) } returns Promise.empty()
			}

			val cachedFilesProvider = mockk<CacheFiles> {
				every { promiseCachedFile(LibraryId(libraryId), "file?key=1") } returns Promise(mockk<File>())
			}

			val cachedAudioFileUriProvider = CachedAudioFileUriProvider(
                remoteFileUriProvider,
                cachedFilesProvider
            )

			cachedAudioFileUriProvider
				.promiseUri(LibraryId(libraryId), ServiceFile(10))
				.toExpiringFuture()
				.get()
		}
	}

	@Test
	fun thenTheUriIsEmpty() {
		assertThat(cachedFileUri).isNull()
	}
}
