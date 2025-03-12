package com.lasthopesoftware.bluewater.client.access.jriver.GivenAJRiverConnection.AndItReturnsARandomErrorCode

import com.lasthopesoftware.bluewater.client.access.JRiverLibraryAccess
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.connection.FakeConnectionResponseTuple
import com.lasthopesoftware.bluewater.client.connection.FakeJRiverConnectionProvider
import com.lasthopesoftware.bluewater.shared.exceptions.HttpResponseException
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.util.Random
import java.util.concurrent.ExecutionException

class WhenSendingPlayedToServer {
	private val expectedResponseCode by lazy {
		val random = Random()
		random.nextInt(300, 600)
	}

	private val updater by lazy {
		val connectionProvider = FakeJRiverConnectionProvider()
		connectionProvider.mapResponse({
			FakeConnectionResponseTuple(
				expectedResponseCode, ByteArray(0)
			)
		}, "File/Played", "File=15", "FileType=Key")

        JRiverLibraryAccess(connectionProvider)
	}
	private var httpResponseException: HttpResponseException? = null

	@BeforeAll
	fun act() {
		try {
			updater.promisePlaystatsUpdate(ServiceFile(15)).toExpiringFuture().get()
		} catch (e: ExecutionException) {
			httpResponseException = e.cause as? HttpResponseException
		}
	}

	@Test
	fun thenAnHttpResponseExceptionIsThrownWithTheResponseCode() {
		assertThat(httpResponseException!!.responseCode).isEqualTo(expectedResponseCode)
	}
}
