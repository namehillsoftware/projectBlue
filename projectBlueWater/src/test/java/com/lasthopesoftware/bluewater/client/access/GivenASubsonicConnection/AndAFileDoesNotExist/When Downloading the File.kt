package com.lasthopesoftware.bluewater.client.access.subsonic.GivenASubsonicConnection.AndAFileDoesNotExist

import com.lasthopesoftware.TestUrl
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.connection.SubsonicConnectionDetails
import com.lasthopesoftware.bluewater.client.connection.live.LiveSubsonicConnection
import com.lasthopesoftware.bluewater.client.connection.requests.FakeHttpConnection
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.promises.extensions.toPromise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class `When Downloading the File` {
	private val inputStream by lazy {
		val downloader = LiveSubsonicConnection(
			SubsonicConnectionDetails(TestUrl, "v2oabCzaDZ", "vOAhLOYu"),
			mockk {
				every { promiseServerClient(any<SubsonicConnectionDetails>()) } returns FakeHttpConnection().toPromise()
			},
			mockk(),
			mockk(),
			mockk(),
		)
		downloader.promiseFile(ServiceFile("98e7097c383a412a8c184b6af08e082f")).toExpiringFuture().get()
	}

	@Test
	fun `then an empty input stream is returned`() {
		assertThat(inputStream!!.available()).isEqualTo(0)
	}
}
