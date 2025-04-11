package com.lasthopesoftware.bluewater.client.connection.settings.mediacenter.GivenALibraryWithAnEmptyAccessCode

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.settings.MediaCenterConnectionSettings
import com.lasthopesoftware.bluewater.client.connection.settings.MissingAccessCodeException
import com.lasthopesoftware.bluewater.client.connection.settings.ValidConnectionSettingsLookup
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.util.concurrent.ExecutionException

class `When Getting Valid Media Center Connection Settings` {

	private val mut by lazy {
		ValidConnectionSettingsLookup(
			mockk {
				every { promiseConnectionSettings(LibraryId(10)) } returns Promise(
					MediaCenterConnectionSettings(accessCode = " 		")
				)
			}
		)
	}

	private var exception: MissingAccessCodeException? = null

	@BeforeAll
	fun act() {
		try {
			mut.promiseConnectionSettings(LibraryId(10)).toExpiringFuture().get()
		} catch (e: ExecutionException) {
			exception = e.cause as? MissingAccessCodeException
		}
	}

	@Test
	fun `then a missing access code exception is thrown`() {
		assertThat(exception).isNotNull
	}
}
