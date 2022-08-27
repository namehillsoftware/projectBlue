package com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.playstats.playedfile.GivenAConnectionThatReturnsARandomErrorCode

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.playstats.playedfile.PlayedFilePlayStatsUpdater
import com.lasthopesoftware.bluewater.client.connection.FakeConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.FakeConnectionResponseTuple
import com.lasthopesoftware.bluewater.shared.exceptions.HttpResponseException
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.util.*
import java.util.concurrent.ExecutionException

class WhenSendingPlayedToServer {

	private val updater by lazy {
		val random = Random()
		do {
			expectedResponseCode = random.nextInt()
		} while (expectedResponseCode < 0 || expectedResponseCode in 200..299)
		val connectionProvider = FakeConnectionProvider()
		connectionProvider.mapResponse({
			FakeConnectionResponseTuple(
				expectedResponseCode, ByteArray(0)
			)
		}, "File/Played", "File=15", "FileType=Key")
		PlayedFilePlayStatsUpdater(connectionProvider)
	}

	private var httpResponseException: HttpResponseException? = null
	private var expectedResponseCode = 0

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
