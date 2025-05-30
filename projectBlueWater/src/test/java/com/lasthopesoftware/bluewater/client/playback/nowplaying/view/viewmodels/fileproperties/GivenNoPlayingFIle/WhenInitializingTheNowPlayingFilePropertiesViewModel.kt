package com.lasthopesoftware.bluewater.client.playback.nowplaying.view.viewmodels.fileproperties.GivenNoPlayingFIle

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.authentication.CheckIfConnectionIsReadOnly
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.ManageNowPlayingState
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.NowPlaying
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.viewmodels.NowPlayingFilePropertiesViewModel
import com.lasthopesoftware.bluewater.client.playback.service.ControlPlaybackService
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.promises.extensions.toPromise
import com.lasthopesoftware.resources.RecordingTypedMessageBus
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

private const val libraryId = 11

class WhenInitializingTheNowPlayingFilePropertiesViewModel {

	private val nowPlayingViewModel by lazy {
		val nowPlayingRepository = mockk<ManageNowPlayingState> {
			every { promiseNowPlaying(LibraryId(libraryId)) } returns Promise(
				NowPlaying(
					LibraryId(libraryId),
					emptyList(),
					0,
					649,
					false
				)
			)
		}

		val checkAuthentication = mockk<CheckIfConnectionIsReadOnly> {
			every { promiseIsReadOnly(LibraryId(libraryId)) } returns true.toPromise()
		}

		val playbackService = mockk<ControlPlaybackService> {
			every { promiseIsMarkedForPlay(LibraryId(libraryId)) } returns true.toPromise()
		}

		val nowPlayingViewModel = NowPlayingFilePropertiesViewModel(
			mockk(relaxed = true, relaxUnitFun = true),
			nowPlayingRepository,
			mockk(),
			mockk(),
			mockk(),
			checkAuthentication,
			playbackService,
			mockk(),
			mockk(relaxed = true) {
				every { nothingPlaying } returns "Nada"
			},
			RecordingTypedMessageBus(),
		)

		nowPlayingViewModel
	}

	@BeforeAll
	fun act() {
		nowPlayingViewModel.initializeViewModel(LibraryId(libraryId)).toExpiringFuture().get()
	}

	@Test
	fun `then the file position is correct`() {
		assertThat(nowPlayingViewModel.filePosition.value).isEqualTo(649)
	}

	@Test
	fun `then the artist is correct`() {
		assertThat(nowPlayingViewModel.artist.value).isEqualTo("")
	}

	@Test
	fun `then the title is correct`() {
		assertThat(nowPlayingViewModel.title.value).isEqualTo("Nada")
	}

	@Test
	fun `then the rating is correct`() {
		assertThat(nowPlayingViewModel.songRating.value).isEqualTo(0f)
	}

	@Test
	fun `then the rating is disabled`() {
		assertThat(nowPlayingViewModel.isSongRatingEnabled.value).isFalse
	}
}
