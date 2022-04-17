package com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.playstats.playedfile.GivenAConnectionThatReturnsARandomErrorCode

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.playstats.playedfile.PlayedFilePlayStatsUpdater
import com.lasthopesoftware.bluewater.client.connection.FakeConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.FakeConnectionResponseTuple
import com.lasthopesoftware.bluewater.shared.exceptions.HttpResponseException
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import org.assertj.core.api.Assertions.assertThat
import org.junit.BeforeClass
import org.junit.Test
import java.util.*
import java.util.concurrent.ExecutionException

class WhenSendingPlayedToServer {
	companion object {
		private var httpResponseException: HttpResponseException? = null
		private var expectedResponseCode = 0

		@BeforeClass
		@JvmStatic
		fun before() {
			val random = Random()
			do {
				expectedResponseCode = random.nextInt()
			} while (expectedResponseCode < 0 || expectedResponseCode >= 200 && expectedResponseCode < 300)
			val connectionProvider = FakeConnectionProvider()
			connectionProvider.mapResponse({
				FakeConnectionResponseTuple(
					expectedResponseCode, ByteArray(0)
				)
			}, "File/Played", "File=15", "FileType=Key")
			val updater = PlayedFilePlayStatsUpdater(connectionProvider)
			try {
				updater.promisePlaystatsUpdate(ServiceFile(15)).toExpiringFuture().get()
			} catch (e: ExecutionException) {
				httpResponseException = e.cause as? HttpResponseException
			}
		}
	}

	@Test
	fun thenAnHttpResponseExceptionIsThrownWithTheResponseCode() {
		assertThat(httpResponseException!!.responseCode).isEqualTo(expectedResponseCode)
	}
}
