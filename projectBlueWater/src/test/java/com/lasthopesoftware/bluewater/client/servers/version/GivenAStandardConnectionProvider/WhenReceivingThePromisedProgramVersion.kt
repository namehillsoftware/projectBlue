package com.lasthopesoftware.bluewater.client.servers.version.GivenAStandardConnectionProvider

import com.lasthopesoftware.bluewater.client.connection.FakeConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.FakeConnectionResponseTuple
import com.lasthopesoftware.bluewater.client.servers.version.ProgramVersionProvider
import com.lasthopesoftware.bluewater.client.servers.version.SemanticVersion
import com.lasthopesoftware.bluewater.shared.promises.extensions.toFuture
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.util.concurrent.TimeUnit
import kotlin.random.Random.Default.nextInt

class WhenReceivingThePromisedProgramVersion {

	companion object {
		private val expectedVersion = lazy {
			SemanticVersion(nextInt(), nextInt(), nextInt())
		}

		private val version = lazy {
			val connectionProvider = FakeConnectionProvider()

			connectionProvider.mapResponse({
				FakeConnectionResponseTuple(
					200,
					("<Response Status=\"OK\">" +
						"<Item Name=\"RuntimeGUID\">{7FF5918E-9FDE-4D4D-9AE7-62DFFDD64397}</Item>" +
						"<Item Name=\"LibraryVersion\">24</Item><Item Name=\"ProgramName\">JRiver Media Center</Item>" +
						"<Item Name=\"ProgramVersion\">" + expectedVersion.value + "</Item>" +
						"<Item Name=\"FriendlyName\">Media-Pc</Item>" +
						"<Item Name=\"AccessKey\">nIpfQr</Item>" +
						"</Response>").toByteArray()
				)
			}, "Alive")
			val programVersionProvider = ProgramVersionProvider(connectionProvider)
			programVersionProvider.promiseServerVersion().toFuture()[100, TimeUnit.MILLISECONDS]
		}
	}

	@Test
	fun thenTheServerVersionIsPresent() {
		assertThat(version.value).isNotNull
	}

	@Test
	fun thenTheServerVersionIsCorrect() {
		assertThat(version.value).isEqualTo(expectedVersion.value)
	}
}