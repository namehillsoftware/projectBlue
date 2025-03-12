package com.lasthopesoftware.bluewater.client.access.jriver.GivenAJRiverConnection.AndAFileDoesNotReturnData

import com.lasthopesoftware.bluewater.client.access.JRiverLibraryAccess
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.connection.ProvideConnections
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.promises.extensions.toPromise
import io.mockk.every
import io.mockk.mockk
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class `When Downloading the File` {
	private val inputStream by lazy {
		val builder = Request.Builder()
		builder.url("http://stuff/")
		val responseBuilder = Response.Builder()
		responseBuilder
			.request(builder.build())
			.protocol(Protocol.HTTP_1_1)
			.code(202)
			.message("Not Found")
			.body("".toResponseBody())
		val fakeConnectionProvider = mockk<ProvideConnections> {
			every { promiseResponse(*anyVararg()) } returns responseBuilder.build().toPromise()
		}

		val downloader = JRiverLibraryAccess(fakeConnectionProvider)
		downloader.promiseFile(ServiceFile(4)).toExpiringFuture().get()
	}

	@Test
	fun `then an empty input stream is returned`() {
		assertThat(inputStream!!.available()).isEqualTo(0)
	}
}
