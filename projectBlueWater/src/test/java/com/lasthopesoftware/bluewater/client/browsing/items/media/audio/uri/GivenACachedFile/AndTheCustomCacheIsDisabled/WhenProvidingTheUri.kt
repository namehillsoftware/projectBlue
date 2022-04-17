package com.lasthopesoftware.bluewater.client.browsing.items.media.audio.uri.GivenACachedFile.AndTheCustomCacheIsDisabled

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.playback.caching.uri.CachedAudioFileUriProvider
import com.lasthopesoftware.bluewater.settings.repository.ApplicationSettings
import com.lasthopesoftware.bluewater.settings.repository.access.HoldApplicationSettings
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WhenProvidingTheUri {
	companion object {
		private val cachedFileUri by lazy {
			val applicationSettings = mockk<HoldApplicationSettings>().apply {
				every { promiseApplicationSettings() } returns Promise(ApplicationSettings())
			}

			val cachedAudioFileUriProvider = CachedAudioFileUriProvider(
				applicationSettings,
				mockk(),
				mockk()
			)

			cachedAudioFileUriProvider
				.promiseFileUri(ServiceFile(10))
				.toExpiringFuture()
				.get()
		}
	}

	@Test
	fun `then the uri is not provided`() {
		assertThat(cachedFileUri).isNull()
	}
}
