package com.lasthopesoftware.bluewater.client.browsing.files.properties.playstats.playedfile.GivenAStandardConnection

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.properties.playstats.playedfile.PlayedFilePlayStatsUpdater
import com.lasthopesoftware.bluewater.client.connection.FakeConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.FakeConnectionResponseTuple
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class WhenSendingPlayedToServer {

	private val functionEnded by lazy {
		val connectionProvider = FakeConnectionProvider()
		connectionProvider.mapResponse({
			FakeConnectionResponseTuple(
				200,
				ByteArray(0)
			)
		}, "File/Played", "File=15", "FileType=Key")

		val updater = PlayedFilePlayStatsUpdater(connectionProvider)
		updater.promisePlaystatsUpdate(ServiceFile(15)).toExpiringFuture().get()
	}

	@Test
	fun thenTheFileIsUpdated() {
		assertThat(functionEnded).isEqualTo(Unit)
	}
}
