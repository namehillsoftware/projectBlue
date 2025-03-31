package com.lasthopesoftware.bluewater.client.playback.nowplaying.view.viewmodels.screen.GivenTheNowPlayingScreenIsLoaded

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.playback.file.PositionedFile
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.NowPlayingMessage
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.viewmodels.InMemoryNowPlayingDisplaySettings
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.viewmodels.NowPlayingScreenViewModel
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.messages.LibraryPlaybackMessage
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.promises.extensions.toPromise
import com.lasthopesoftware.resources.RecordingApplicationMessageBus
import com.lasthopesoftware.resources.RecordingTypedMessageBus
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import java.util.concurrent.TimeUnit


class `When A File Track Changes` {
	companion object {
		private val libraryId = LibraryId(508)
	}

	private val mut by lazy {
		val nowPlayingMessageBus = RecordingTypedMessageBus<NowPlayingMessage>()
		val applicationMessages = RecordingApplicationMessageBus()
		val vm = NowPlayingScreenViewModel(
			applicationMessages,
			nowPlayingMessageBus,
			InMemoryNowPlayingDisplaySettings(),
			mockk {
				every { promiseIsMarkedForPlay(libraryId) } returns false.toPromise()
			},
		)

		Pair(applicationMessages, vm)
	}

	private var testStartTime = 0L

	@BeforeAll
	fun act() {
		testStartTime = System.currentTimeMillis()
		mut.second.initializeViewModel(libraryId).toExpiringFuture().get()
		mut.first.sendMessage(
			LibraryPlaybackMessage.TrackChanged(
				libraryId,
				PositionedFile(159, ServiceFile("995"))))
	}

	@Test
	fun `then the screen is off`() {
		assertThat(mut.second.isScreenOn.value).isFalse()
	}

	@Test
	@Timeout(10, unit = TimeUnit.SECONDS)
	fun `then the controls are shown at least five seconds after the properties load`() {
		mut.second.isScreenControlsVisible.skipWhile { !it.value }.takeWhile { it.value }.blockingSubscribe()
		assertThat(System.currentTimeMillis() - testStartTime).isGreaterThanOrEqualTo(5_000)
	}
}
