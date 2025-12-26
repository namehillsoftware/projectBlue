package com.lasthopesoftware.bluewater.client.playback.nowplaying.view.viewmodels.fileproperties.GivenAPlayingFile.AndAnInitializedViewModel.AndTheFileChanges.AndAConnectionErrorOccursEventually.AndTheConnectionIsRestored

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.properties.MappedFilePropertiesLookup
import com.lasthopesoftware.bluewater.client.browsing.files.properties.NormalizedFileProperties
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.authentication.CheckIfConnectionIsReadOnly
import com.lasthopesoftware.bluewater.client.connection.session.LibraryConnectionChangedMessage
import com.lasthopesoftware.bluewater.client.connection.url.UrlKeyHolder
import com.lasthopesoftware.bluewater.client.playback.file.PositionedFile
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.ManageNowPlayingState
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.NowPlaying
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.viewmodels.NowPlayingFilePropertiesViewModel
import com.lasthopesoftware.bluewater.client.playback.service.ControlPlaybackService
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.messages.LibraryPlaybackMessage
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.observables.toCloseable
import com.lasthopesoftware.promises.extensions.toPromise
import com.lasthopesoftware.resources.RecordingApplicationMessageBus
import com.lasthopesoftware.resources.RecordingTypedMessageBus
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.io.EOFException
import java.net.URL

class WhenHandlingTheFileChange {

	companion object {
		private const val libraryId = 630
		private const val firstServiceFileId = "179"
		private const val secondServiceFileId = "36"
	}

	private val mut by lazy {
		val nowPlayingRepository = mockk<ManageNowPlayingState> {
            every { promiseNowPlaying(LibraryId(libraryId)) } returns Promise(
                NowPlaying(
                    LibraryId(libraryId),
                    listOf(
                        ServiceFile("815"),
                        ServiceFile("449"),
                        ServiceFile("592"),
                        ServiceFile(firstServiceFileId),
                        ServiceFile("390"),
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
            every { promiseIsMarkedForPlay(LibraryId(libraryId)) } returns true.toPromise()
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
                } returns MappedFilePropertiesLookup().toPromise()
                every {
                    promiseFileProperties(
                        LibraryId(libraryId),
                        ServiceFile(secondServiceFileId)
                    )
                } returnsMany listOf(
					Promise(EOFException("Oof")),
					Promise(EOFException("Uff")),
					MappedFilePropertiesLookup(mapOf(
						Pair(NormalizedFileProperties.Artist, "cow"),
						Pair(NormalizedFileProperties.Name, "spill"),
						Pair(NormalizedFileProperties.Rating, "591"),
					)).toPromise(),
				)
            },
            mockk {
                every { promiseGuaranteedUrlKey(LibraryId(libraryId), any<ServiceFile>()) } answers {
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
			RecordingTypedMessageBus(),
        )

		nowPlayingViewModel.initializeViewModel(LibraryId(libraryId)).toExpiringFuture().get()

		every { nowPlayingRepository.promiseNowPlaying(LibraryId(libraryId)) } returns Promise(
            NowPlaying(
                LibraryId(libraryId),
                listOf(
                    ServiceFile("815"),
                    ServiceFile("449"),
                    ServiceFile("592"),
                    ServiceFile(firstServiceFileId),
                    ServiceFile("390"),
                    ServiceFile(secondServiceFileId),
                ),
                5,
                605,
                false,
            )
        )

		Pair(messageBus, nowPlayingViewModel)
	}

	private val nowPlayingStates = mutableListOf<Boolean>()

	@BeforeAll
	fun act() {
		val (messageBus, vm) = mut
		messageBus.sendMessage(
            LibraryPlaybackMessage.TrackChanged(
                LibraryId(libraryId),
                PositionedFile(5, ServiceFile(secondServiceFileId))
            )
		)

		try {
			vm.isPlaying.subscribe { nowPlayingStates.add(it.value) }.toCloseable().use {
				// Add another initialize to catch edge cases where this would happen and cancel the connection changed promise
				// (but not reset the promise).
				vm.initializeViewModel(LibraryId(libraryId)).toExpiringFuture().get()
			}
		} catch(_: Throwable) {
			// ignored
		}

		messageBus.sendMessage(LibraryConnectionChangedMessage(LibraryId(libraryId)))
	}

	@Test
	fun `then the is playing state isn't disrupted`() {
		assertThat(nowPlayingStates).containsExactly(true)
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
