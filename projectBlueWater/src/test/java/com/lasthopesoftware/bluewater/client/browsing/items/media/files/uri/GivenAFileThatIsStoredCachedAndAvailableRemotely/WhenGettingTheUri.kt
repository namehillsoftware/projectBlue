package com.lasthopesoftware.bluewater.client.browsing.items.media.files.uri.GivenAFileThatIsStoredCachedAndAvailableRemotely

import android.net.Uri
import com.lasthopesoftware.bluewater.client.browsing.items.media.audio.uri.CachedAudioFileUriProvider
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.uri.BestMatchUriProvider
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.uri.RemoteFileUriProvider
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.stored.library.items.files.system.uri.MediaFileUriProvider
import com.lasthopesoftware.bluewater.client.stored.library.items.files.uri.StoredFileUriProvider
import com.lasthopesoftware.bluewater.shared.promises.extensions.toFuture
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class WhenGettingTheUri {

	companion object {
		private val returnedFileUri by lazy {
			val mockStoredFileUriProvider = mockk<StoredFileUriProvider>()
			every { mockStoredFileUriProvider.promiseFileUri(any()) } returns Promise(Uri.fromFile(File("/a_path/to_a_file.mp3")))

			val cachedAudioFileUriProvider = mockk<CachedAudioFileUriProvider>()
			every { cachedAudioFileUriProvider.promiseFileUri(ServiceFile(3)) } returns Promise(Uri.fromFile(File("/a_cached_path/to_a_file.mp3")))

			val mockMediaFileUriProvider = mockk<MediaFileUriProvider>()
			every { mockMediaFileUriProvider.promiseFileUri(any()) } returns Promise(Uri.fromFile(File("/a_media_path/to_a_file.mp3")))

			val mockRemoteFileUriProvider = mockk<RemoteFileUriProvider>()
			every { mockRemoteFileUriProvider.promiseFileUri(ServiceFile(3)) } returns Promise(Uri.parse("http://remote-url/to_a_file.mp3"))

			val bestMatchUriProvider = BestMatchUriProvider(
				Library(),
				mockStoredFileUriProvider,
				cachedAudioFileUriProvider,
				mockMediaFileUriProvider,
				mockRemoteFileUriProvider
			)
			bestMatchUriProvider
				.promiseFileUri(ServiceFile(3))
				.toFuture()
				.get()
		}
	}

	@Test
	fun thenTheStoredFileUriIsReturned() {
		assertThat(returnedFileUri.toString())
			.isEqualTo("file:///a_path/to_a_file.mp3")
	}
}
