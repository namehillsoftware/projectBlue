package com.lasthopesoftware.bluewater.client.servers.version.GivenAStandardConnectionProvider

import com.lasthopesoftware.bluewater.client.connection.FakeConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.FakeConnectionResponseTuple
import com.lasthopesoftware.bluewater.client.connection.url.IUrlProvider
import com.lasthopesoftware.bluewater.client.servers.version.ProgramVersionProvider
import com.lasthopesoftware.bluewater.client.servers.version.SemanticVersion
import com.lasthopesoftware.bluewater.shared.promises.extensions.toFuture
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.BeforeClass
import org.junit.Test
import java.net.URL
import java.util.*
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

class WhenReceivingThePromisedProgramVersion {

	companion object {
		private var version: SemanticVersion? = null
		private var expectedVersion: SemanticVersion? = null

		@BeforeClass
		@Throws(InterruptedException::class, ExecutionException::class, TimeoutException::class)
		fun before() {
			val urlProvider = mockk<IUrlProvider>()
			every { urlProvider.baseUrl } returns URL("")
			val connectionProvider = FakeConnectionProvider()
			val random = Random()
			expectedVersion = SemanticVersion(random.nextInt(), random.nextInt(), random.nextInt())
			connectionProvider.mapResponse({
				FakeConnectionResponseTuple(
					200,
					("<Response Status=\"OK\">" +
						"<Item Name=\"RuntimeGUID\">{7FF5918E-9FDE-4D4D-9AE7-62DFFDD64397}</Item>" +
						"<Item Name=\"LibraryVersion\">24</Item><Item Name=\"ProgramName\">JRiver Media Center</Item>" +
						"<Item Name=\"ProgramVersion\">" + expectedVersion + "</Item>" +
						"<Item Name=\"FriendlyName\">Media-Pc</Item>" +
						"<Item Name=\"AccessKey\">nIpfQr</Item>" +
						"</Response>").toByteArray()
				)
			}, "Alive")
			val programVersionProvider = ProgramVersionProvider(connectionProvider)
			version = programVersionProvider.promiseServerVersion().toFuture()[100, TimeUnit.MILLISECONDS]
		}
	}

	@Test
	fun thenTheServerVersionIsCorrect() {
		assertThat(version).isEqualTo(expectedVersion)
	}
}
