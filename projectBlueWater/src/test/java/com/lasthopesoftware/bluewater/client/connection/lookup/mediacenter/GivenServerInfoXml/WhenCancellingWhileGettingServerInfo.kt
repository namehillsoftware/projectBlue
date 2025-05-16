package com.lasthopesoftware.bluewater.client.connection.lookup.mediacenter.GivenServerInfoXml

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.lookup.ServerLookup
import com.lasthopesoftware.bluewater.client.connection.settings.MediaCenterConnectionSettings
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.io.IOException
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit

class WhenCancellingWhileGettingServerInfo {

	private val mut by lazy {
		ServerLookup(
			mockk {
				every { promiseConnectionSettings(any()) } returns MediaCenterConnectionSettings(accessCode = "").toPromise()
			},
			mockk {
				every { promiseServerInfoXml(any()) } returns Promise { m ->
					m.awaitCancellation {
						m.sendRejection(IOException("This was no good"))
					}
				}
			},
        )
	}

	private lateinit var error: IOException

	@BeforeAll
	fun act() {
		val promisedServerInfo = mut.promiseServerInformation(LibraryId(10))
		promisedServerInfo.cancel()
		try {
			promisedServerInfo.toExpiringFuture()[5, TimeUnit.SECONDS]
		} catch (ee: ExecutionException) {
			error = ee.cause as? IOException ?: throw ee
		}
	}

	@Test
	fun `then the exception is correct`() {
		assertThat(error.message).isEqualTo("This was no good")
	}
}
