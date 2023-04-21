package com.lasthopesoftware.bluewater.client.playback.nowplaying.view.actvity.viewmodels.GivenAPlayingFile

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.properties.ProvideFreshLibraryFileProperties
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.authentication.CheckIfConnectionIsReadOnly
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.MaintainNowPlayingState
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.NowPlaying
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.activity.viewmodels.NowPlayingFilePropertiesViewModel
import com.lasthopesoftware.bluewater.client.playback.service.ControlPlaybackService
import com.lasthopesoftware.bluewater.shared.UrlKeyHolder
import com.lasthopesoftware.bluewater.shared.promises.PromiseDelay
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.dropWhile
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.joda.time.Duration
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import java.net.URL
import java.util.concurrent.TimeUnit

private const val libraryId = 718
private const val serviceFileId = 355

class WhenInitializingTheNowPlayingFilePropertiesViewModel {

	private var filePropertiesReturnedTime = 0L

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

		val filePropertiesProvider = mockk<ProvideFreshLibraryFileProperties> {
			val delayedPromise by lazy { PromiseDelay.delay<Any>(Duration.standardSeconds(1)) }
			every { promiseFileProperties(LibraryId(libraryId), ServiceFile(serviceFileId)) } answers {
				delayedPromise.then {
					filePropertiesReturnedTime = System.currentTimeMillis()
					emptyMap()
				}
			}
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

		nowPlayingViewModel
	}

	@BeforeAll
	fun act() {
		nowPlayingViewModel.initializeViewModel().toExpiringFuture().get()
	}

	@Test
	fun `then the file position is correct`() {
		assertThat(nowPlayingViewModel.filePosition.value).isEqualTo(439774)
	}

	@Test
	@Timeout(10, unit = TimeUnit.SECONDS)
	fun `then the controls are shown at least five seconds after the properties load`() {
		runBlocking {
			nowPlayingViewModel.isScreenControlsVisible.dropWhile { !it }.takeWhile { it }.collect()
		}
		assertThat(System.currentTimeMillis() - filePropertiesReturnedTime).isGreaterThan(5_000)
	}
}
