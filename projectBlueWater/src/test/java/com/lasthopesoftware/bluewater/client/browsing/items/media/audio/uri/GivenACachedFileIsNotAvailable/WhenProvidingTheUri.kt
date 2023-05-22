package com.lasthopesoftware.bluewater.client.browsing.items.media.audio.uri.GivenACachedFileIsNotAvailable

import android.net.Uri
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.cached.CacheFiles
import com.lasthopesoftware.bluewater.client.browsing.files.uri.RemoteFileUriProvider
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
				every { promiseUri(ServiceFile(10)) } returns Promise(Uri.parse("http://a-url/file?key=1"))
			}

			val cachedFilesProvider = mockk<CacheFiles>().apply {
				every { promiseCachedFile("/file?key=1") } returns Promise.empty()
			}

			val cachedAudioFileUriProvider = CachedAudioFileUriProvider(
                remoteFileUriProvider,
                cachedFilesProvider
            )

			cachedAudioFileUriProvider
				.promiseUri(ServiceFile(10))
				.toExpiringFuture()
				.get()
		}
    }

	@Test
	fun thenTheUriIsEmpty() {
		assertThat(cachedFileUri).isNull()
	}
}
