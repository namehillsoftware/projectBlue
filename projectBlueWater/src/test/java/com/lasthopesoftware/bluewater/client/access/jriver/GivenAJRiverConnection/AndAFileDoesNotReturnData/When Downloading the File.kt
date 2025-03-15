package com.lasthopesoftware.bluewater.client.access.jriver.GivenAJRiverConnection.AndAFileDoesNotReturnData

import com.lasthopesoftware.bluewater.client.access.JRiverLibraryAccess
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.connection.ProvideConnections
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.promises.extensions.toPromise
import com.lasthopesoftware.resources.PassThroughHttpResponse
import com.lasthopesoftware.resources.emptyByteArray
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class `When Downloading the File` {
	private val inputStream by lazy {
		val fakeConnectionProvider = mockk<ProvideConnections> {
			every { promiseResponse(any(), *anyVararg()) } returns PassThroughHttpResponse(202, "Not found", emptyByteArray.inputStream()).toPromise()
		}

		val downloader = JRiverLibraryAccess(fakeConnectionProvider)
		downloader.promiseFile(ServiceFile(4)).toExpiringFuture().get()
	}

	@Test
	fun `then an empty input stream is returned`() {
		assertThat(inputStream!!.available()).isEqualTo(0)
	}
}
