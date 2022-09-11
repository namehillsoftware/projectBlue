package com.lasthopesoftware.bluewater.client.playback.view.nowplaying.activity.viewmodels.GivenAPlayingFile

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.properties.KnownFileProperties
import com.lasthopesoftware.bluewater.client.browsing.files.properties.ProvideLibraryFileProperties
import com.lasthopesoftware.bluewater.client.browsing.files.properties.storage.FilePropertyUpdatedMessage
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.authentication.CheckIfScopedConnectionIsReadOnly
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.MaintainNowPlayingState
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.NowPlaying
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.activity.viewmodels.NowPlayingFilePropertiesViewModel
import com.lasthopesoftware.bluewater.client.playback.service.ControlPlaybackService
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.lasthopesoftware.resources.RecordingApplicationMessageBus
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class WhenItsPropertiesChanges {

	private val playlist = listOf(
		ServiceFile(71),
		ServiceFile(614),
		ServiceFile(252),
		ServiceFile(643),
		ServiceFile(409),
		ServiceFile(1000),
		ServiceFile(188),
		ServiceFile(118),
	)

	private val playlistPosition
		get() = 708 % playlist.size

	private val services by lazy {
		val nowPlayingRepository = mockk<MaintainNowPlayingState>().apply {
			every { promiseNowPlaying() } returns Promise(
				NowPlaying(
					LibraryId(697),
					playlist,
					playlistPosition,
					439774,
					false
				)
			)
		}

		val filePropertiesProvider = mockk<ProvideLibraryFileProperties> {
			every { promiseFileProperties(LibraryId(697), playlist[playlistPosition]) } returnsMany listOf(
				mapOf(
					Pair(KnownFileProperties.ARTIST, "block"),
					Pair(KnownFileProperties.NAME, "tongue"),
				).toPromise(),
				mapOf(
					Pair(KnownFileProperties.ARTIST, "plan"),
					Pair(KnownFileProperties.NAME, "honor"),
				).toPromise(),
			)
		}

		val checkAuthentication = mockk<CheckIfScopedConnectionIsReadOnly>().apply {
			every { promiseIsReadOnly() } returns true.toPromise()
		}

		val playbackService = mockk<ControlPlaybackService>().apply {
			every { promiseIsMarkedForPlay() } returns true.toPromise()
		}

		val messageBus = RecordingApplicationMessageBus()

		val nowPlayingViewModel = NowPlayingFilePropertiesViewModel(
			messageBus,
			nowPlayingRepository,
			mockk {
				every { selectedLibraryId } returns Promise(LibraryId(697))
			},
			filePropertiesProvider,
			mockk(),
			checkAuthentication,
			playbackService,
			mockk(),
			mockk(relaxed = true),
		)

		Pair(messageBus, nowPlayingViewModel)
	}

	private val viewModel
		get() = services.second

	@BeforeAll
	fun act() {
		val (messageBus, viewModel) = services
		viewModel.initializeViewModel().toExpiringFuture().get()
		messageBus.sendMessage(FilePropertyUpdatedMessage(LibraryId(697), playlist[playlistPosition]))
	}

	@Test
	fun `then the file position is correct`() {
		assertThat(viewModel.filePosition.value).isEqualTo(439774)
	}

	@Test
	fun `then the artist is correct`() {
		assertThat(viewModel.artist.value).isEqualTo("plan")
	}

	@Test
	fun `then the name is correct`() {
		assertThat(viewModel.title.value).isEqualTo("honor")
	}
}
