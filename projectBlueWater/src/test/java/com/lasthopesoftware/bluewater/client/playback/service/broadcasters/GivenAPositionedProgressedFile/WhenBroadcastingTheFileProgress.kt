package com.lasthopesoftware.bluewater.client.playback.service.broadcasters.GivenAPositionedProgressedFile

import androidx.test.core.app.ApplicationProvider
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.FakeScopedCachedFilesPropertiesProvider
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.KnownFileProperties
import com.lasthopesoftware.bluewater.client.playback.file.PositionedProgressedFile
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.TrackPositionBroadcaster
import com.lasthopesoftware.resources.FakeMessageBus
import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.joda.time.Duration
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WhenBroadcastingTheFileProgress {

	companion object {
		private val receivedIntent by lazy {
			val messageBus = FakeMessageBus(ApplicationProvider.getApplicationContext())
			val fileProperties = FakeScopedCachedFilesPropertiesProvider()
			fileProperties.addFilePropertiesToCache(
				ServiceFile(880),
				mapOf(Pair(KnownFileProperties.DURATION, ".389"))
			)
			val trackPositionBroadcaster = TrackPositionBroadcaster(messageBus, fileProperties)
			trackPositionBroadcaster.broadcastProgress(
				PositionedProgressedFile(
					313,
					ServiceFile(880),
					Duration.millis(45955),
				)
			)
			messageBus.recordedIntents.first()
		}

		private val duration by lazy {
			receivedIntent.getLongExtra(TrackPositionBroadcaster.TrackPositionChangedParameters.fileDuration, -1)
		}

		private val progress by lazy {
			receivedIntent.getLongExtra(TrackPositionBroadcaster.TrackPositionChangedParameters.filePosition, -1)
		}
	}

	@Test
	fun thenTheProgressIsCorrect() {
		assertThat(progress).isEqualTo(45955)
	}

	@Test
	fun thenTheDurationIsCorrect() {
		assertThat(duration).isEqualTo(389)
	}
}
