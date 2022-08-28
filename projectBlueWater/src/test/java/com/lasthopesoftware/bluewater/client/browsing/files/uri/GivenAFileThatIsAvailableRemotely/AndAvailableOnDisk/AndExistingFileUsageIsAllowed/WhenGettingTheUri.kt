package com.lasthopesoftware.bluewater.client.browsing.files.uri.GivenAFileThatIsAvailableRemotely.AndAvailableOnDisk.AndExistingFileUsageIsAllowed

import android.net.Uri
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.uri.BestMatchUriProvider
import com.lasthopesoftware.bluewater.client.browsing.files.uri.RemoteFileUriProvider
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
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

@RunWith(RobolectricTestRunner::class)
class WhenGettingTheUri {

    companion object {
        private val returnedFileUri by lazy {
			val mockStoredFileUriProvider = mockk<StoredFileUriProvider>()
			every { mockStoredFileUriProvider.promiseFileUri(any()) } returns Promise.empty()

			val cachedAudioFileUriProvider = mockk<CachedAudioFileUriProvider>()
			every { cachedAudioFileUriProvider.promiseFileUri(ServiceFile(3)) } returns Promise.empty()

			val mockMediaFileUriProvider = mockk<MediaFileUriProvider>()
			every { mockMediaFileUriProvider.promiseFileUri(any()) } returns Promise(Uri.fromFile(File("/a_media_path/to_a_file.mp3")))

			val mockRemoteFileUriProvider = mockk<RemoteFileUriProvider>()
			every { mockRemoteFileUriProvider.promiseFileUri(ServiceFile(3)) } returns Promise(Uri.parse("http://remote-url/to_a_file.mp3"))

			val bestMatchUriProvider = BestMatchUriProvider(
				Library().setIsUsingExistingFiles(true),
				mockStoredFileUriProvider,
				cachedAudioFileUriProvider,
				mockMediaFileUriProvider,
				mockRemoteFileUriProvider
			)

			bestMatchUriProvider
				.promiseFileUri(ServiceFile(3))
				.toExpiringFuture()
				.get()
		}
    }

	@Test
	fun thenTheMediaFileUriIsReturned() {
		assertThat(returnedFileUri.toString())
			.isEqualTo("file:///a_media_path/to_a_file.mp3")
	}
}
