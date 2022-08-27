package com.lasthopesoftware.bluewater.client.playback.service.broadcasters.GivenAPositionedProgressedFile

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.FakeScopedCachedFilesPropertiesProvider
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.KnownFileProperties
import com.lasthopesoftware.bluewater.client.playback.file.PositionedProgressedFile
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.TrackPositionBroadcaster
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.messages.TrackPositionUpdate
import com.lasthopesoftware.resources.RecordingApplicationMessageBus
import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.joda.time.Duration
import org.junit.Test

class WhenBroadcastingTheFileProgress {

	private val receivedMessage by lazy {
		val appMessageBus = RecordingApplicationMessageBus()
		val fileProperties = FakeScopedCachedFilesPropertiesProvider()
		fileProperties.addFilePropertiesToCache(
			ServiceFile(880),
			mapOf(Pair(KnownFileProperties.DURATION, ".389"))
		)
		val trackPositionBroadcaster = TrackPositionBroadcaster(appMessageBus, fileProperties)
		trackPositionBroadcaster.broadcastProgress(
			PositionedProgressedFile(
				313,
				ServiceFile(880),
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
