package com.lasthopesoftware.bluewater.client.access.jriver.GivenAJRiverConnection

import com.lasthopesoftware.bluewater.client.access.JRiverLibraryAccess
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.connection.FakeConnectionResponseTuple
import com.lasthopesoftware.bluewater.client.connection.FakeJRiverConnectionProvider
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import org.apache.commons.io.IOUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.util.Random

class `When Downloading a File` {
	private val responseBytes by lazy {
		val bytes = ByteArray(400)
		Random().nextBytes(bytes)
		bytes
	}

	private val inputStream by lazy {
		val fakeConnectionProvider = FakeJRiverConnectionProvider()
		fakeConnectionProvider.mapResponse({
			FakeConnectionResponseTuple(
				200,
				responseBytes
			)
		})

		val downloader = JRiverLibraryAccess(fakeConnectionProvider)
		downloader.promiseFile(ServiceFile(4)).toExpiringFuture().get()
	}

	@Test
	fun `then the input stream is returned`() {
		val outputStream = ByteArrayOutputStream()
		IOUtils.copy(inputStream, outputStream)
		assertThat(outputStream.toByteArray()).containsExactly(*responseBytes)
	}
}
