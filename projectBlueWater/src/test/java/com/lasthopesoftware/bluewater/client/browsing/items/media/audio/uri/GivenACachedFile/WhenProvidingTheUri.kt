package com.lasthopesoftware.bluewater.client.browsing.items.media.audio.uri.GivenACachedFile

import android.net.Uri
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.cached.access.ICachedFilesProvider
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.cached.repository.CachedFile
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.uri.RemoteFileUriProvider
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

@RunWith(RobolectricTestRunner::class)
class WhenProvidingTheUri {
	companion object {
		private val file by lazy {
			File.createTempFile("temp", ".txt").apply { deleteOnExit() }
		}

		private val cachedFileUri by lazy {
			val remoteUri = Uri.parse("http://a-url/file?key=1")
			val remoteFileUriProvider = mockk<RemoteFileUriProvider>().apply {
				every { promiseFileUri(ServiceFile(10)) } returns Promise(remoteUri)
			}

			val cachedFilesProvider = mockk<ICachedFilesProvider>().apply {
				every { promiseCachedFile(remoteUri.path + "?" + remoteUri.query) } returns Promise(CachedFile().setFileName(file.absolutePath))
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
	fun thenTheUriIsThePathToTheFile() {
		assertThat(cachedFileUri.toString())
			.isEqualTo(Uri.fromFile(file).toString())
	}
}