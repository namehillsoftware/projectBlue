package com.lasthopesoftware.bluewater.client.browsing.files.uri.GivenAFileThatIsCachedAndAvailableRemotely.AndAvailableOnDisk.AndExistingFileUsageIsNotAllowed

import android.net.Uri
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.uri.BestMatchUriProvider
import com.lasthopesoftware.bluewater.client.browsing.files.uri.RemoteFileUriProvider
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.playback.caching.uri.CachedAudioFileUriProvider
import com.lasthopesoftware.bluewater.client.stored.library.items.files.system.uri.MediaFileUriProvider
import com.lasthopesoftware.bluewater.client.stored.library.items.files.uri.StoredFileUriProvider
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

private const val libraryId = 366

@RunWith(RobolectricTestRunner::class)
class WhenGettingTheUri {

	companion object {
		val serviceFile = 3
		private val returnedFileUri by lazy {
			val mockStoredFileUriProvider = mockk<StoredFileUriProvider>()
			every { mockStoredFileUriProvider.promiseUri(any(), any()) } returns Promise.empty()

			val cachedAudioFileUriProvider = mockk<CachedAudioFileUriProvider>()
			every { cachedAudioFileUriProvider.promiseUri(LibraryId(libraryId), ServiceFile(serviceFile)) } returns Promise(Uri.fromFile(File("/a_cached_path/to_a_file.mp3")))

			val mockMediaFileUriProvider = mockk<MediaFileUriProvider>()
			every { mockMediaFileUriProvider.promiseUri(any(), any()) } returns Promise(Uri.fromFile(File("/a_media_path/to_a_file.mp3")))

			val mockRemoteFileUriProvider = mockk<RemoteFileUriProvider>()
			every { mockRemoteFileUriProvider.promiseUri(LibraryId(libraryId), ServiceFile(serviceFile)) } returns Promise(Uri.parse("http://remote-url/to_a_file.mp3"))

			val bestMatchUriProvider = BestMatchUriProvider(
				mockk {
					every { promiseLibrary(LibraryId(libraryId)) } returns Promise(Library())
				},
				mockStoredFileUriProvider,
				cachedAudioFileUriProvider,
				mockMediaFileUriProvider,
				mockRemoteFileUriProvider
			)

			bestMatchUriProvider
				.promiseUri(LibraryId(libraryId), ServiceFile(serviceFile))
				.toExpiringFuture()
				.get()
		}
	}

	@Test
	fun thenTheCachedFileUriIsReturned() {
		assertThat(returnedFileUri.toString())
			.isEqualTo("file:///a_cached_path/to_a_file.mp3")
	}
}
