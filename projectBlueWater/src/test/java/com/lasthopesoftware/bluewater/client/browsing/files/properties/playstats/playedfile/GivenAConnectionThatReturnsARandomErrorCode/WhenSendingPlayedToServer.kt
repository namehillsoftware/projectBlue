package com.lasthopesoftware.bluewater.client.browsing.files.properties.playstats.playedfile.GivenAConnectionThatReturnsARandomErrorCode

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.properties.playstats.playedfile.PlayedFilePlayStatsUpdater
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.FakeConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.FakeConnectionResponseTuple
import com.lasthopesoftware.bluewater.shared.exceptions.HttpResponseException
import com.lasthopesoftware.bluewater.shared.promises.extensions.ProgressingPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.util.*
import java.util.concurrent.ExecutionException

private const val libraryId = 72

class WhenSendingPlayedToServer {

	private val expectedResponseCode by lazy {
		val random = Random()
		var responseCode: Int
		do {
			responseCode = random.nextInt()
		} while (responseCode < 0 || responseCode in 200..299)
		responseCode
	}

	private val updater by lazy {
		PlayedFilePlayStatsUpdater(mockk {
			val connectionProvider = FakeConnectionProvider()
			connectionProvider.mapResponse({
				FakeConnectionResponseTuple(
					expectedResponseCode, ByteArray(0)
				)
			}, "File/Played", "File=15", "FileType=Key")

			every { promiseLibraryConnection(LibraryId(libraryId)) } returns ProgressingPromise(connectionProvider)
		})
	}
	private var httpResponseException: HttpResponseException? = null

	@BeforeAll
	fun act() {
		try {
			updater.promisePlaystatsUpdate(LibraryId(libraryId), ServiceFile(15)).toExpiringFuture().get()
		} catch (e: ExecutionException) {
			httpResponseException = e.cause as? HttpResponseException
		}
	}

	@Test
	fun thenAnHttpResponseExceptionIsThrownWithTheResponseCode() {
		assertThat(httpResponseException!!.responseCode).isEqualTo(expectedResponseCode)
	}
}
