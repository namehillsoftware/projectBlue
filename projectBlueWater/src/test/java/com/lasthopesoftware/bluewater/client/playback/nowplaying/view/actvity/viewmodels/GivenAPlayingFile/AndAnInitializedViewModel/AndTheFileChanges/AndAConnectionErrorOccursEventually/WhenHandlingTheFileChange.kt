package com.lasthopesoftware.bluewater.client.playback.nowplaying.view.actvity.viewmodels.GivenAPlayingFile.AndAnInitializedViewModel.AndTheFileChanges.AndAConnectionErrorOccursEventually

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.authentication.CheckIfConnectionIsReadOnly
import com.lasthopesoftware.bluewater.client.playback.file.PositionedFile
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.MaintainNowPlayingState
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.NowPlaying
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.activity.viewmodels.NowPlayingFilePropertiesViewModel
import com.lasthopesoftware.bluewater.client.playback.service.ControlPlaybackService
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.messages.PlaybackMessage
import com.lasthopesoftware.bluewater.shared.UrlKeyHolder
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.lasthopesoftware.resources.RecordingApplicationMessageBus
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.io.EOFException
import java.net.URL

private const val libraryId = 151
private const val firstServiceFileId = 344
private const val secondServiceFileId = 342

class WhenHandlingTheFileChange {

	private val mut by lazy {
		val nowPlayingRepository = mockk<MaintainNowPlayingState> {
			every { promiseNowPlaying() } returns Promise(
				NowPlaying(
					LibraryId(libraryId),
					listOf(
						ServiceFile(815),
						ServiceFile(449),
						ServiceFile(592),
						ServiceFile(firstServiceFileId),
						ServiceFile(390),
						ServiceFile(secondServiceFileId),
					),
					3,
					439774,
					false
				)
			)
		}

		val checkAuthentication = mockk<CheckIfConnectionIsReadOnly> {
			every { promiseIsReadOnly(LibraryId(libraryId)) } returns true.toPromise()
		}

		val playbackService = mockk<ControlPlaybackService> {
			every { promiseIsMarkedForPlay() } returns true.toPromise()
		}

		val messageBus = RecordingApplicationMessageBus()

		val nowPlayingViewModel = NowPlayingFilePropertiesViewModel(
            messageBus,
            nowPlayingRepository,
            mockk {
				every { promiseFileProperties(LibraryId(libraryId), ServiceFile(firstServiceFileId)) } returns emptyMap<String, String>().toPromise()
				every { promiseFileProperties(LibraryId(libraryId), ServiceFile(secondServiceFileId)) } returns Promise(
					EOFException("Oof")
				)
			},
            mockk {
                every { promiseUrlKey(LibraryId(libraryId), any<ServiceFile>()) } answers {
					UrlKeyHolder(
						URL("http://plan"),
						lastArg<ServiceFile>()
					).toPromise()
				}
            },
            mockk(),
            checkAuthentication,
            playbackService,
            mockk(),
            mockk(relaxed = true),
		)

		nowPlayingViewModel.initializeViewModel().toExpiringFuture().get()

		every { nowPlayingRepository.promiseNowPlaying() } returns Promise(
			NowPlaying(
				LibraryId(libraryId),
				listOf(
					ServiceFile(815),
					ServiceFile(449),
					ServiceFile(592),
					ServiceFile(firstServiceFileId),
					ServiceFile(390),
					ServiceFile(secondServiceFileId),
				),
				5,
				605,
				false
			)
		)

		Pair(messageBus, nowPlayingViewModel)
	}

	@BeforeAll
	fun act() {
		val (messageBus, _) = mut
		messageBus.sendMessage(
			PlaybackMessage.TrackChanged(
				LibraryId(libraryId),
				PositionedFile(5, ServiceFile(secondServiceFileId))
			)
		)
	}

	@Test
	fun `then the file position is 0 because the track duration could not be loaded`() {
		assertThat(mut.second.filePosition.value).isEqualTo(0)
	}

	@Test
	fun `then the error is expected`() {
		assertThat(mut.second.unexpectedError.value).isNull()
	}
}
