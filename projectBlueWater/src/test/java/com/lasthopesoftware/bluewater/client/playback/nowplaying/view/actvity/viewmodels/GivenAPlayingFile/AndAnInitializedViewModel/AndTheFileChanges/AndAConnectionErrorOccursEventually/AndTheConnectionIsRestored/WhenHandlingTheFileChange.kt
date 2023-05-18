package com.lasthopesoftware.bluewater.client.playback.nowplaying.view.actvity.viewmodels.GivenAPlayingFile.AndAnInitializedViewModel.AndTheFileChanges.AndAConnectionErrorOccursEventually.AndTheConnectionIsRestored

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.properties.KnownFileProperties
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.authentication.CheckIfConnectionIsReadOnly
import com.lasthopesoftware.bluewater.client.connection.session.initialization.LibraryConnectionChangedMessage
import com.lasthopesoftware.bluewater.client.playback.file.PositionedFile
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.MaintainNowPlayingState
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.NowPlaying
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.viewmodels.NowPlayingFilePropertiesViewModel
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

private const val libraryId = 630
private const val firstServiceFileId = 179
private const val secondServiceFileId = 36

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
                every {
                    promiseFileProperties(
                        LibraryId(libraryId),
                        ServiceFile(firstServiceFileId)
                    )
                } returns emptyMap<String, String>().toPromise()
                every {
                    promiseFileProperties(
                        LibraryId(libraryId),
                        ServiceFile(secondServiceFileId)
                    )
                } returnsMany listOf(
					Promise(EOFException("Oof")),
					mapOf(
						Pair(KnownFileProperties.Artist, "cow"),
						Pair(KnownFileProperties.Name, "spill"),
						Pair(KnownFileProperties.Rating, "591"),
					).toPromise(),
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
            mockk {
				every { pollConnection(LibraryId(libraryId)) } returns Promise(Exception("fail"))
			},
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

		messageBus.sendMessage(LibraryConnectionChangedMessage(LibraryId(libraryId)))
	}

	@Test
	fun `then the file position is correct`() {
		assertThat(mut.second.filePosition.value).isEqualTo(605)
	}

	@Test
	fun `then the error is expected`() {
		assertThat(mut.second.unexpectedError.value).isNull()
	}

	@Test
	fun `then the artist is correct`() {
		assertThat(mut.second.artist.value).isEqualTo("cow")
	}

	@Test
	fun `then the title is correct`() {
		assertThat(mut.second.title.value).isEqualTo("spill")
	}
}
