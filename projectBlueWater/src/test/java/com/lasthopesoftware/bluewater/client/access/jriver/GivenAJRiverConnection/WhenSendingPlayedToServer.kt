package com.lasthopesoftware.bluewater.client.access.jriver.GivenAJRiverConnection

import com.lasthopesoftware.bluewater.client.access.JRiverLibraryAccess
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.connection.FakeConnectionResponseTuple
import com.lasthopesoftware.bluewater.client.connection.FakeJRiverConnectionProvider
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.resources.emptyByteArray
import com.namehillsoftware.handoff.promises.Promise
import okhttp3.Response
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class WhenSendingPlayedToServer {

	private var isFilePlayedCalled = false
	private val updater by lazy {
		val connectionProvider = object : FakeJRiverConnectionProvider() {
			override fun promiseResponse(path: String, vararg params: String): Promise<Response> {
				isFilePlayedCalled = path == "File/Played" && params.contentEquals(arrayOf("File=15", "FileType=Key"))
				return super.promiseResponse(path, *params)
			}
		}
		connectionProvider.mapResponse(
			{ FakeConnectionResponseTuple(200, emptyByteArray) },
			"File/Played",
			"File=15",
			"FileType=Key"
		)

        JRiverLibraryAccess(connectionProvider)
	}

	@BeforeAll
	fun act() {
		updater.promisePlaystatsUpdate(ServiceFile(15)).toExpiringFuture().get()
	}

	@Test
	fun `then the file is updated`() {
		assertThat(isFilePlayedCalled).isTrue
	}
}
