package com.lasthopesoftware.bluewater.shared.android.mediasession.GivenAPartiallyPlayedPlaylist

import com.lasthopesoftware.AndroidContext
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.playback.nowplaying.FakeNowPlayingRepository
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.NowPlaying
import com.lasthopesoftware.bluewater.shared.android.MediaSession.MediaSessionController
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class WhenInitializingTheMediaSessionController : AndroidContext() {
	companion object {
		private const val libraryId = 40
		private const val serviceFileId = "407"
		private var isActivated = false
		private var isDeactivatedAfterActivating = false
	}

	override fun before() {
		MediaSessionController(
			mockk(relaxUnitFun = true) {
				every { isActive } answers {
					isActivated
				}

				every { isActive = any() } propertyType Boolean::class answers {
					if (value) isActivated = true
					else isDeactivatedAfterActivating = isActivated
				}
			},
			FakeNowPlayingRepository(NowPlaying(
				LibraryId(libraryId),
				listOf(ServiceFile(serviceFileId)),
				1,
				1L,
				false,
			))
		).use {
			it.promiseInitialization().toExpiringFuture().get()
		}
	}

	@Test
	fun `then the session is activated`() {
		assertThat(isActivated).isTrue
	}

	@Test
	fun `then the session is deactivated after activating`() {
		assertThat(isDeactivatedAfterActivating).isTrue
	}
}
