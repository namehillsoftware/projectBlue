package com.lasthopesoftware.bluewater.client.connection.live.GivenANetworkExists.AndMissingConnectionSettings

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.live.ConfiguredActiveNetwork
import com.lasthopesoftware.bluewater.client.connection.live.LiveServerConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.live.MissingConnectionSettingsException
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.resources.strings.JsonEncoderDecoder
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.util.concurrent.ExecutionException

class `When Getting The Live Connection` {

	private val services by lazy {
		LiveServerConnectionProvider(
			ConfiguredActiveNetwork(isNetworkActive = true),
			mockk(),
			mockk(),
			mockk {
				every { promiseConnectionSettings(any()) } returns Promise.empty()
			},
			mockk(),
			mockk(),
			mockk(),
			mockk(),
			JsonEncoderDecoder,
			mockk(),
		)
	}

	private var exception: MissingConnectionSettingsException? = null

	@BeforeAll
	fun before() {
		try {
			services.promiseLiveServerConnection(LibraryId(32)).toExpiringFuture().get()
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
