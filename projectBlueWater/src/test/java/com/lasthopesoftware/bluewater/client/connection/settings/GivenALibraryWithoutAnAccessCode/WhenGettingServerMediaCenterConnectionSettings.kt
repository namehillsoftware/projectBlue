package com.lasthopesoftware.bluewater.client.connection.settings.GivenALibraryWithoutAnAccessCode

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.browsing.library.settings.LibrarySettings
import com.lasthopesoftware.bluewater.client.browsing.library.settings.StoredMediaCenterConnectionSettings
import com.lasthopesoftware.bluewater.client.connection.settings.ConnectionSettingsLookup
import com.lasthopesoftware.bluewater.client.connection.settings.MissingAccessCodeException
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.util.concurrent.ExecutionException

class WhenGettingServerMediaCenterConnectionSettings {

	private val mut by lazy {
		ConnectionSettingsLookup(
			mockk {
				every { promiseLibrarySettings(LibraryId(10)) } returns Promise(
					LibrarySettings(
						libraryId = LibraryId(10),
						connectionSettings = StoredMediaCenterConnectionSettings()
					)
				)
			}
		)
	}

	private var exception: MissingAccessCodeException? = null

	@BeforeAll
	fun act() {
		try {
			mut.lookupConnectionSettings(LibraryId(10)).toExpiringFuture().get()
		} catch (e: ExecutionException) {
			exception = e.cause as? MissingAccessCodeException
		}
	}

	@Test
	fun `then a missing access code exception is thrown`() {
		assertThat(exception).isNotNull
	}
}
