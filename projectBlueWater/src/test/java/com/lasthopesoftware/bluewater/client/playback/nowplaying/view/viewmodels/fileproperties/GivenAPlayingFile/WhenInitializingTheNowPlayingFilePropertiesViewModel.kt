package com.lasthopesoftware.bluewater.client.playback.nowplaying.view.viewmodels.fileproperties.GivenAPlayingFile

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.properties.ProvideFreshLibraryFileProperties
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.authentication.CheckIfConnectionIsReadOnly
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.MaintainNowPlayingState
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.NowPlaying
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.NowPlayingMessage
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.viewmodels.NowPlayingFilePropertiesViewModel
import com.lasthopesoftware.bluewater.client.playback.service.ControlPlaybackService
import com.lasthopesoftware.bluewater.shared.UrlKeyHolder
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.promises.PromiseDelay
import com.lasthopesoftware.promises.extensions.toPromise
import com.lasthopesoftware.resources.RecordingTypedMessageBus
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.joda.time.Duration
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.net.URL

private const val libraryId = 718
private const val serviceFileId = 355

class WhenInitializingTheNowPlayingFilePropertiesViewModel {

	private var filePropertiesReturnedTime = 0L

	private val mut by lazy {
		val nowPlayingRepository = mockk<MaintainNowPlayingState> {
			every { promiseNowPlaying(LibraryId(libraryId)) } returns Promise(
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
				delayedPromise.then { _ ->
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

		val nowPlayingMessageBus = RecordingTypedMessageBus<NowPlayingMessage>()

		val nowPlayingViewModel = NowPlayingFilePropertiesViewModel(
            mockk(relaxed = true, relaxUnitFun = true),
            nowPlayingRepository,
            filePropertiesProvider,
            mockk {
                every { promiseGuaranteedUrlKey(LibraryId(libraryId), ServiceFile(serviceFileId)) } returns UrlKeyHolder(
					URL("http://plan"),
					ServiceFile(serviceFileId)
				).toPromise()
            },
            mockk(),
            checkAuthentication,
            playbackService,
			mockk(),
			mockk(relaxed = true),
			nowPlayingMessageBus,
		)

		Pair(nowPlayingMessageBus, nowPlayingViewModel)
	}

	@BeforeAll
	fun act() {
		mut.second.initializeViewModel(LibraryId(libraryId)).toExpiringFuture().get()
	}

	@Test
	fun `then the file position is correct`() {
		assertThat(mut.second.filePosition.value).isEqualTo(439774)
	}

	@Test
	fun `then the file properties loaded message is sent`() {
		assertThat(mut.first.recordedMessages).containsExactly(NowPlayingMessage.FilePropertiesLoaded)
	}
}
