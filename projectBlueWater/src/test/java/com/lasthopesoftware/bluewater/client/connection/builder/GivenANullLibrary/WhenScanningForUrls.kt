package com.lasthopesoftware.bluewater.client.connection.builder.GivenANullLibrary

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.builder.MissingConnectionSettingsException
import com.lasthopesoftware.bluewater.client.connection.builder.UrlScanner
import com.lasthopesoftware.bluewater.client.connection.settings.LookupConnectionSettings
import com.lasthopesoftware.bluewater.shared.promises.extensions.toFuture
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.BeforeClass
import org.junit.Test
import java.util.concurrent.ExecutionException

class WhenScanningForUrls {

	companion object {
		private var exception: MissingConnectionSettingsException? = null

		@BeforeClass
		@JvmStatic
		fun before() {
			val connectionSettingsLookup = mockk<LookupConnectionSettings>()
			every { connectionSettingsLookup.lookupConnectionSettings(any()) } returns Promise.empty()

			val urlScanner = UrlScanner(
				mockk(),
				mockk(),
				mockk(),
				connectionSettingsLookup,
				mockk()
			)
			try {
				urlScanner.promiseBuiltUrlProvider(LibraryId(32)).toFuture().get()
			} catch (e: ExecutionException) {
				exception = e.cause as? MissingConnectionSettingsException ?: throw e
			}
		}
	}

	@Test
	fun thenTheCorrectExceptionIsThrown() {
		assertThat(exception).isNotNull
	}

	@Test
	fun thenTheExceptionMentionsTheLibrary() {
		assertThat(exception?.message).isEqualTo("Connection settings were not found for ${LibraryId(32)}")
	}
}
