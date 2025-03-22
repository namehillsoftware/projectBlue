package com.lasthopesoftware.bluewater.client.playback.nowplaying.view.viewmodels.fileproperties.GivenAPlayingFile.AndAnInitializedViewModel.AndThePlaylistChanges.AndTheFileIsNull

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.properties.KnownFileProperties
import com.lasthopesoftware.bluewater.client.browsing.files.properties.ProvideFreshLibraryFileProperties
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.authentication.CheckIfConnectionIsReadOnly
import com.lasthopesoftware.bluewater.client.connection.url.UrlKeyHolder
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.MaintainNowPlayingState
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.NowPlaying
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.viewmodels.NowPlayingFilePropertiesViewModel
import com.lasthopesoftware.bluewater.client.playback.service.ControlPlaybackService
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.messages.LibraryPlaybackMessage
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.promises.extensions.toPromise
import com.lasthopesoftware.resources.RecordingApplicationMessageBus
import com.lasthopesoftware.resources.RecordingTypedMessageBus
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.net.URL

private const val libraryId = 687

class WhenHandlingThePlaylistChange {

	private val mut by lazy {
		val nowPlayingRepository = mockk<MaintainNowPlayingState> {
			every { promiseNowPlaying(LibraryId(libraryId)) } returns Promise(
				NowPlaying(
					LibraryId(libraryId),
					listOf(
						ServiceFile(5)
					),
					0,
					649,
					false
				)
			) andThen Promise(
				NowPlaying(
					LibraryId(libraryId),
					emptyList(),
					0,
					992,
					false
				)
			)
		}

		val filePropertiesProvider = mockk<ProvideFreshLibraryFileProperties> {
			every {
				promiseFileProperties(
					LibraryId(libraryId),
					ServiceFile(5)
				)
			} returns mapOf(
				Pair(KnownFileProperties.Artist, "tea"),
				Pair(KnownFileProperties.Name, "rake"),
				Pair(KnownFileProperties.Rating, "748"),
			).toPromise()
		}

		val checkAuthentication = mockk<CheckIfConnectionIsReadOnly> {
			every { promiseIsReadOnly(LibraryId(libraryId)) } returns true.toPromise()
		}

		val playbackService = mockk<ControlPlaybackService> {
			every { promiseIsMarkedForPlay(LibraryId(libraryId)) } returns true.toPromise()
		}

		val recordingApplicationMessageBus = RecordingApplicationMessageBus()
		val nowPlayingViewModel = NowPlayingFilePropertiesViewModel(
			recordingApplicationMessageBus,
			nowPlayingRepository,
			filePropertiesProvider,
			mockk {
				every { promiseGuaranteedUrlKey(LibraryId(libraryId), ServiceFile(5)) } returns Promise(
					UrlKeyHolder(URL("http://77Q8Tq2h/"), ServiceFile(5))
				)
			},
			mockk(),
			checkAuthentication,
			playbackService,
			mockk(),
			mockk(relaxed = true) {
				every { nothingPlaying } returns "Nada"
			},
			RecordingTypedMessageBus(),
		)

		Pair(recordingApplicationMessageBus, nowPlayingViewModel)
	}

	@BeforeAll
	fun act() {
		val (messageBus, viewModel) = mut
		viewModel.initializeViewModel(LibraryId(libraryId)).toExpiringFuture().get()
		messageBus.sendMessage(LibraryPlaybackMessage.PlaylistChanged(LibraryId(libraryId)))
	}

	@Test
	fun `then the file position is correct`() {
		assertThat(mut.second.filePosition.value).isEqualTo(992)
	}

	@Test
	fun `then the artist is correct`() {
		assertThat(mut.second.artist.value).isEqualTo("")
	}

	@Test
	fun `then the title is correct`() {
		assertThat(mut.second.title.value).isEqualTo("Nada")
	}

	@Test
	fun `then the rating is correct`() {
		assertThat(mut.second.songRating.value).isEqualTo(0f)
	}

	@Test
	fun `then the rating is disabled`() {
		assertThat(mut.second.isSongRatingEnabled.value).isFalse
	}
}
