package com.lasthopesoftware.bluewater.client.playback.nowplaying.view.viewmodels.fileproperties.GivenNoPlayingFIle

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.properties.ProvideFreshLibraryFileProperties
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.authentication.CheckIfConnectionIsReadOnly
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.MaintainNowPlayingState
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.NowPlaying
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.viewmodels.NowPlayingFilePropertiesViewModel
import com.lasthopesoftware.bluewater.client.playback.service.ControlPlaybackService
import com.lasthopesoftware.bluewater.shared.promises.PromiseDelay
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.joda.time.Duration
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

private const val libraryId = 11

class WhenInitializingTheNowPlayingFilePropertiesViewModel {

	private var filePropertiesReturnedTime = 0L

	private val nowPlayingViewModel by lazy {
		val nowPlayingRepository = mockk<MaintainNowPlayingState> {
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

		val filePropertiesProvider = mockk<ProvideFreshLibraryFileProperties> {
            val delayedPromise by lazy { PromiseDelay.delay<Any>(Duration.standardSeconds(1)) }
            every {
                promiseFileProperties(
                    LibraryId(libraryId),
                    ServiceFile(149)
                )
            } answers {
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
            every { promiseIsMarkedForPlay(LibraryId(libraryId)) } returns true.toPromise()
        }

		val nowPlayingViewModel = NowPlayingFilePropertiesViewModel(
            mockk(relaxed = true, relaxUnitFun = true),
            nowPlayingRepository,
            filePropertiesProvider,
            mockk(),
            mockk(),
            checkAuthentication,
            playbackService,
            mockk(),
            mockk(relaxed = true) {
		  		every { nothingPlaying } returns "Nada"
			},
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
