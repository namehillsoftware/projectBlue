package com.lasthopesoftware.bluewater.client.playback.nowplaying.view.actvity.viewmodels.GivenAPlayingFile

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.properties.FakeFilesPropertiesProvider
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.authentication.CheckIfConnectionIsReadOnly
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.MaintainNowPlayingState
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.NowPlaying
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.activity.viewmodels.NowPlayingFilePropertiesViewModel
import com.lasthopesoftware.bluewater.client.playback.service.ControlPlaybackService
import com.lasthopesoftware.bluewater.shared.UrlKeyHolder
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.net.URL

private const val libraryId = 718
private const val serviceFileId = 355

class WhenInitializingTheNowPlayingFilePropertiesViewModel {

	private val nowPlayingViewModel by lazy {
		val nowPlayingRepository = mockk<MaintainNowPlayingState> {
			every { promiseNowPlaying() } returns Promise(
				NowPlaying(
					LibraryId(libraryId),
					listOf(
						ServiceFile(815),
						ServiceFile(449),
						ServiceFile(592),
						ServiceFile(serviceFileId),
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
				ServiceFile(serviceFileId),
				LibraryId(libraryId),
				emptyMap()
			)
		}

		val checkAuthentication = mockk<CheckIfConnectionIsReadOnly> {
			every { promiseIsReadOnly(LibraryId(libraryId)) } returns true.toPromise()
		}

		val playbackService = mockk<ControlPlaybackService> {
			every { promiseIsMarkedForPlay() } returns true.toPromise()
		}

		val nowPlayingViewModel = NowPlayingFilePropertiesViewModel(
            mockk(relaxed = true, relaxUnitFun = true),
            nowPlayingRepository,
            filePropertiesProvider,
            mockk {
                every { promiseUrlKey(LibraryId(libraryId), ServiceFile(serviceFileId)) } returns UrlKeyHolder(
					URL("http://plan"),
					ServiceFile(serviceFileId)
				).toPromise()
            },
            mockk(),
            checkAuthentication,
            playbackService,
            mockk(),
            mockk(relaxed = true),
		)

		nowPlayingViewModel.initializeViewModel().toExpiringFuture().get()

		nowPlayingViewModel
	}

	@Test
	fun `then the file position is correct`() {
		assertThat(nowPlayingViewModel.filePosition.value).isEqualTo(439774)
	}
}
