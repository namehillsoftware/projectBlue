package com.lasthopesoftware.bluewater.client.playback.view.nowplaying.activity.viewmodels.GivenAPlayingFile

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.properties.FakeScopedCachedFilesPropertiesProvider
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.FakeConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.authentication.CheckIfScopedConnectionIsReadOnly
import com.lasthopesoftware.bluewater.client.connection.selected.ProvideSelectedConnection
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
		val nowPlayingRepository = mockk<MaintainNowPlayingState>().apply {
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

		val connectionProvider = mockk<ProvideSelectedConnection>().apply {
			every { promiseSessionConnection() } returns Promise(FakeConnectionProvider())
		}

		val filePropertiesProvider = FakeScopedCachedFilesPropertiesProvider().apply {
			addFilePropertiesToCache(
				ServiceFile(355),
				emptyMap()
			)
		}

		val checkAuthentication = mockk<CheckIfScopedConnectionIsReadOnly>().apply {
			every { promiseIsReadOnly() } returns true.toPromise()
		}

		val playbackService = mockk<ControlPlaybackService>().apply {
			every { promiseIsMarkedForPlay() } returns true.toPromise()
		}

		val nowPlayingViewModel = NowPlayingFilePropertiesViewModel(
			mockk(relaxed = true, relaxUnitFun = true),
			nowPlayingRepository,
			connectionProvider,
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
