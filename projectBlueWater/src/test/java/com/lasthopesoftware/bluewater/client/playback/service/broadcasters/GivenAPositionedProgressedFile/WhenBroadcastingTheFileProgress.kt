package com.lasthopesoftware.bluewater.client.playback.service.broadcasters.GivenAPositionedProgressedFile

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.properties.FakeFilesPropertiesProvider
import com.lasthopesoftware.bluewater.client.browsing.files.properties.NormalizedFileProperties
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.playback.file.PositionedProgressedFile
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.TrackPositionBroadcaster
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.messages.TrackPositionUpdate
import com.lasthopesoftware.resources.RecordingApplicationMessageBus
import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.joda.time.Duration
import org.junit.jupiter.api.Test

class WhenBroadcastingTheFileProgress {

	private val receivedMessage by lazy {
		val appMessageBus = RecordingApplicationMessageBus()
		val fileProperties = FakeFilesPropertiesProvider()
		fileProperties.addFilePropertiesToCache(
			ServiceFile("880"),
			LibraryId(758),
			mapOf(Pair(NormalizedFileProperties.Duration, ".389"))
		)
		val trackPositionBroadcaster = TrackPositionBroadcaster(appMessageBus, fileProperties)
		trackPositionBroadcaster.broadcastProgress(
			LibraryId(758),
			PositionedProgressedFile(
				313,
				ServiceFile("880"),
				Duration.millis(45955),
			)
		)

		appMessageBus.recordedMessages.first() as? TrackPositionUpdate
	}

	private val duration by lazy {
		receivedMessage?.fileDuration?.millis
	}

	private val progress by lazy {
		receivedMessage?.filePosition?.millis
	}

	@Test
	fun `then the progress is correct`() {
		assertThat(progress).isEqualTo(45955)
	}

	@Test
	fun `then the duration is correct`() {
		assertThat(duration).isEqualTo(389)
	}
}
