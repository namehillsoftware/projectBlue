package com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.playstats.playedfile.GivenAStandardConnection

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.playstats.playedfile.PlayedFilePlayStatsUpdater
import com.lasthopesoftware.bluewater.client.connection.FakeConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.FakeConnectionResponseTuple
import com.lasthopesoftware.bluewater.shared.promises.extensions.toFuture
import org.assertj.core.api.Assertions
import org.junit.Test

class WhenSendingPlayedToServer {

	companion object {
		private val functionEnded by lazy {
			val connectionProvider = FakeConnectionProvider()
			connectionProvider.mapResponse({
				FakeConnectionResponseTuple(
					200,
					ByteArray(0)
				)
			}, "File/Played", "File=15", "FileType=Key")

			val updater = PlayedFilePlayStatsUpdater(connectionProvider)
			updater.promisePlaystatsUpdate(ServiceFile(15)).toFuture().get()
		}
	}

	@Test
	fun thenTheFileIsUpdated() {
		Assertions.assertThat(functionEnded).isNull()
	}
}