package com.lasthopesoftware.bluewater.client.connection.builder.lookup.GivenNoServerInfoXml

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.builder.lookup.ServerLookup
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.util.concurrent.CancellationException
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit

class WhenCancellingWhileGettingServerInfo {

	private val mut by lazy {
		ServerLookup(
			mockk {
				every { lookupConnectionSettings(any()) } returns Promise { m ->
					m.awaitCancellation {
						m.sendRejection(CancellationException("Bye now!"))
					}
				}
			},
			mockk {
				every { promiseServerInfoXml(any()) } returns Promise.empty()
			})
	}

	private lateinit var error: CancellationException

	@BeforeAll
	fun act() {
		val promisedServerInfo = mut.promiseServerInformation(LibraryId(10))
		promisedServerInfo.cancel()
		try {
			promisedServerInfo.toExpiringFuture()[5, TimeUnit.SECONDS]
		} catch (ee: ExecutionException) {
			error = ee.cause as? CancellationException ?: throw ee
		}
	}

	@Test
	fun `then the exception is correct`() {
		assertThat(error.message).isEqualTo("Bye now!")
	}
}
