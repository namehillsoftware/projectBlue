package com.lasthopesoftware.bluewater.client.browsing.files.properties.playstats.playedfile.GivenAConnectionThatReturnsA404ErrorCode

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.properties.playstats.playedfile.ScopedPlayedFilePlayStatsUpdater
import com.lasthopesoftware.bluewater.client.connection.FakeConnectionProvider
import com.lasthopesoftware.bluewater.shared.exceptions.HttpResponseException
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.util.concurrent.ExecutionException

class WhenSendingPlayedToServer {

	private var httpResponseException: HttpResponseException? = null

	@BeforeAll
	fun act() {
		val connectionProvider = FakeConnectionProvider()
		val updater = ScopedPlayedFilePlayStatsUpdater(connectionProvider)
		try {
			updater.promisePlaystatsUpdate(ServiceFile(15)).toExpiringFuture().get()
		} catch (e: ExecutionException) {
			httpResponseException = e.cause as? HttpResponseException?
		}
	}

	@Test
	fun thenAnHttpResponseExceptionIsThrownWithTheResponseCode() {
		assertThat(httpResponseException!!.responseCode).isEqualTo(404)
	}
}
