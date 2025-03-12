package com.lasthopesoftware.bluewater.client.access.jriver.GivenAJRiverConnection

import com.lasthopesoftware.bluewater.client.access.JRiverLibraryAccess
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.connection.FakeConnectionResponseTuple
import com.lasthopesoftware.bluewater.client.connection.FakeJRiverConnectionProvider
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.namehillsoftware.handoff.promises.Promise
import okhttp3.Response
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class WhenSendingPlayedToServer {

	private var isFilePlayedCalled = false
	private val updater by lazy {
		val connectionProvider = object : FakeJRiverConnectionProvider() {
			override fun promiseResponse(vararg params: String): Promise<Response> {
				isFilePlayedCalled = params.contentEquals(arrayOf("File/Played", "File=15", "FileType=Key"))
				return super.promiseResponse(*params)
			}
		}
		connectionProvider.mapResponse({
			FakeConnectionResponseTuple(
				200,
				ByteArray(0)
			)
		}, "File/Played", "File=15", "FileType=Key")

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
