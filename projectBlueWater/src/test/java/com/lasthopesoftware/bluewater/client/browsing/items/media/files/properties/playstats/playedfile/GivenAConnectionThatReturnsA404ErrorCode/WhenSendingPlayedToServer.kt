package com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.playstats.playedfile.GivenAConnectionThatReturnsA404ErrorCode

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.playstats.playedfile.PlayedFilePlayStatsUpdater
import com.lasthopesoftware.bluewater.client.connection.FakeConnectionProvider
import com.lasthopesoftware.bluewater.shared.exceptions.HttpResponseException
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import org.assertj.core.api.Assertions.assertThat
import org.junit.BeforeClass
import org.junit.Test
import java.util.concurrent.ExecutionException

class WhenSendingPlayedToServer {

	companion object {
		private var httpResponseException: HttpResponseException? = null

		@BeforeClass
		@JvmStatic
		fun before() {
			val connectionProvider = FakeConnectionProvider()
			val updater = PlayedFilePlayStatsUpdater(connectionProvider)
			try {
				updater.promisePlaystatsUpdate(ServiceFile(15)).toExpiringFuture().get()
			} catch (e: ExecutionException) {
				httpResponseException = e.cause as? HttpResponseException?
			}
		}
	}

	@Test
	fun thenAnHttpResponseExceptionIsThrownWithTheResponseCode() {
		assertThat(httpResponseException!!.responseCode).isEqualTo(404)
	}
}
