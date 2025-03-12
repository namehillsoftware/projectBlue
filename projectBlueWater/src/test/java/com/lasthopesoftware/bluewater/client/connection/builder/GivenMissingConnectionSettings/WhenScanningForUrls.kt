package com.lasthopesoftware.bluewater.client.connection.builder.GivenMissingConnectionSettings

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.builder.MissingConnectionSettingsException
import com.lasthopesoftware.bluewater.client.connection.builder.UrlScanner
import com.lasthopesoftware.bluewater.client.connection.settings.LookupConnectionSettings
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.util.concurrent.ExecutionException

class WhenScanningForUrls {

	private val services by lazy {
		val connectionSettingsLookup = mockk<LookupConnectionSettings>()
		every { connectionSettingsLookup.lookupConnectionSettings(any()) } returns Promise.empty()

		val urlScanner = UrlScanner(
			mockk(),
            mockk(),
			connectionSettingsLookup,
			mockk()
		)

		urlScanner
	}

	private var exception: MissingConnectionSettingsException? = null

	@BeforeAll
	fun before() {
		try {
			services.promiseBuiltUrlProvider(LibraryId(32)).toExpiringFuture().get()
		} catch (e: ExecutionException) {
			exception = e.cause as? MissingConnectionSettingsException ?: throw e
		}
	}

	@Test
	fun `then the correct exception is thrown`() {
		assertThat(exception).isNotNull
	}

	@Test
	fun `then the exception mentions the library`() {
		assertThat(exception?.message).isEqualTo("Connection settings were not found for ${LibraryId(32)}")
	}
}
