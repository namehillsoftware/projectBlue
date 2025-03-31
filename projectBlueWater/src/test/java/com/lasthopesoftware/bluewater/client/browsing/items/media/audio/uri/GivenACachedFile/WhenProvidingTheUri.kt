package com.lasthopesoftware.bluewater.client.browsing.items.media.audio.uri.GivenACachedFile

import android.net.Uri
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.cached.CacheFiles
import com.lasthopesoftware.bluewater.client.browsing.files.uri.RemoteFileUriProvider
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.playback.caching.uri.CachedAudioFileUriProvider
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

private const val libraryId = 408

@RunWith(RobolectricTestRunner::class)
class WhenProvidingTheUri {
	companion object {
		private val file by lazy {
			File.createTempFile("temp", ".txt").apply { deleteOnExit() }
		}

		private val cachedFileUri by lazy {
			val remoteUri = Uri.parse("http://a-url/file?key=1")
			val remoteFileUriProvider = mockk<RemoteFileUriProvider>().apply {
				every { promiseUri(LibraryId(libraryId), ServiceFile("10")) } returns Promise(remoteUri)
			}

			val cachedFilesProvider = mockk<CacheFiles> {
				every { promiseCachedFile(LibraryId(libraryId), remoteUri.path + "?" + remoteUri.query) } returns Promise(file)
			}

			val cachedAudioFileUriProvider = CachedAudioFileUriProvider(
				remoteFileUriProvider,
				cachedFilesProvider
			)

			cachedAudioFileUriProvider
				.promiseUri(LibraryId(libraryId), ServiceFile("10"))
				.toExpiringFuture()
				.get()
		}
	}

	@Test
	fun thenTheUriIsThePathToTheFile() {
		assertThat(cachedFileUri.toString())
			.isEqualTo(Uri.fromFile(file).toString())
	}
}
