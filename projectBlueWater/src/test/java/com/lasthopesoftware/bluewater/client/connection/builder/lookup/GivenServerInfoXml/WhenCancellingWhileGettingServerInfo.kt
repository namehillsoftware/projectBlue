package com.lasthopesoftware.bluewater.client.connection.builder.lookup.GivenServerInfoXml

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.builder.lookup.RequestServerInfoXml
import com.lasthopesoftware.bluewater.client.connection.builder.lookup.ServerLookup
import com.lasthopesoftware.bluewater.shared.promises.extensions.toFuture
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.io.IOException
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit

class WhenCancellingWhileGettingServerInfo {

	companion object {
		private val error by lazy {
			val serverInfoXml = mockk<RequestServerInfoXml>()
			every { serverInfoXml.promiseServerInfoXml(any()) } returns Promise { m ->
				m.cancellationRequested {
					m.sendRejection(IOException("This was no good"))
				}
			}

			val serverLookup = ServerLookup(serverInfoXml)
			val promisedServerInfo = serverLookup.promiseServerInformation(LibraryId(10))
			promisedServerInfo.cancel()
			try {
				promisedServerInfo.toFuture()[5, TimeUnit.SECONDS]
				null
			} catch (ee: ExecutionException) {
				ee.cause as? IOException ?: throw ee
			}
		}
	}

	@Test
	fun thenTheExceptionIsCorrect() {
		assertThat(error?.message).isEqualTo("This was no good")
	}
}
