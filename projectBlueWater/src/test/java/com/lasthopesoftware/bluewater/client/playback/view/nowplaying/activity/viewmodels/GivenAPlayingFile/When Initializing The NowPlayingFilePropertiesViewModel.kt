package com.lasthopesoftware.bluewater.client.playback.view.nowplaying.activity.viewmodels.GivenAPlayingFile

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.properties.FakeFilesPropertiesProvider
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.authentication.CheckIfConnectionIsReadOnly
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.MaintainNowPlayingState
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.NowPlaying
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.activity.viewmodels.NowPlayingFilePropertiesViewModel
import com.lasthopesoftware.bluewater.client.playback.service.ControlPlaybackService
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class `When Initializing The NowPlayingFilePropertiesViewModel` {

	private val nowPlayingViewModel by lazy {
		val nowPlayingRepository = mockk<MaintainNowPlayingState> {
			every { promiseNowPlaying() } returns Promise(
				NowPlaying(
					LibraryId(718),
					listOf(
						ServiceFile(815),
						ServiceFile(449),
						ServiceFile(592),
						ServiceFile(355),
						ServiceFile(390),
					),
					3,
					439774,
					false
				)
			)
		}

		val filePropertiesProvider = FakeFilesPropertiesProvider().apply {
			addFilePropertiesToCache(
				ServiceFile(355),
				LibraryId(718),
				emptyMap()
			)
		}

		val checkAuthentication = mockk<CheckIfConnectionIsReadOnly> {
			every { promiseIsReadOnly(LibraryId(718)) } returns true.toPromise()
		}

		val playbackService = mockk<ControlPlaybackService> {
			every { promiseIsMarkedForPlay() } returns true.toPromise()
		}

		val nowPlayingViewModel = NowPlayingFilePropertiesViewModel(
			mockk(relaxed = true, relaxUnitFun = true),
			nowPlayingRepository,
			mockk {
				every { selectedLibraryId } returns Promise(LibraryId(718))
			},
			filePropertiesProvider,
			mockk(),
			checkAuthentication,
			playbackService,
			mockk(),
			mockk(relaxed = true),
		)

		nowPlayingViewModel.initializeViewModel()

		nowPlayingViewModel
	}

	@Test
	fun `then the file position is correct`() {
		assertThat(nowPlayingViewModel.filePosition.value).isEqualTo(439774)
	}
}
