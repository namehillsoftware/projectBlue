package com.lasthopesoftware.bluewater.client.browsing.files.properties.playstats.playedfile.GivenAStandardConnection

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.properties.playstats.playedfile.PlayedFilePlayStatsUpdater
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.FakeConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.FakeConnectionResponseTuple
import com.lasthopesoftware.bluewater.shared.promises.extensions.ProgressingPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import okhttp3.Response
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

private const val libraryId = 514

class WhenSendingPlayedToServer {

	private val mut by lazy {
		PlayedFilePlayStatsUpdater(mockk {
			val connectionProvider = object : FakeConnectionProvider() {
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

			every { promiseLibraryConnection(LibraryId(libraryId)) } returns ProgressingPromise(connectionProvider)
		})
	}

	private var isFilePlayedCalled = false

	@BeforeAll
	fun act() {
		mut.promisePlaystatsUpdate(LibraryId(libraryId), ServiceFile(15)).toExpiringFuture().get()
	}

	@Test
	fun `then the file is updated`() {
		assertThat(isFilePlayedCalled).isTrue
	}
}
