package com.lasthopesoftware.bluewater.client.servers.version.GivenAStandardConnectionProvider

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.FakeConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.FakeConnectionResponseTuple
import com.lasthopesoftware.bluewater.client.servers.version.LibraryServerVersionProvider
import com.lasthopesoftware.bluewater.client.servers.version.SemanticVersion
import com.lasthopesoftware.bluewater.shared.promises.extensions.ProgressingPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.concurrent.TimeUnit
import kotlin.random.Random.Default.nextInt

class WhenReceivingThePromisedProgramVersion {

	private val expectedVersion by lazy {
		SemanticVersion(nextInt(), nextInt(), nextInt())
	}

	private val version by lazy {
		val connectionProvider = FakeConnectionProvider()

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
		val programVersionProvider = LibraryServerVersionProvider(mockk {
			every { promiseLibraryConnection(LibraryId(506)) } returns ProgressingPromise(connectionProvider)
		})
		programVersionProvider.promiseServerVersion(LibraryId(506)).toExpiringFuture()[100, TimeUnit.MILLISECONDS]
	}

	@Test
	fun `then the server version is present`() {
		assertThat(version).isNotNull
	}

	@Test
	fun `then the server version is correct`() {
		assertThat(version).isEqualTo(expectedVersion)
	}
}
