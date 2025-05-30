package com.lasthopesoftware.bluewater.client.playback.nowplaying.view.viewmodels.fileproperties.GivenAPlayingFile.AndAnInitializedViewModel

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.properties.NormalizedFileProperties
import com.lasthopesoftware.bluewater.client.browsing.files.properties.ProvideFreshLibraryFileProperties
import com.lasthopesoftware.bluewater.client.browsing.files.properties.storage.FilePropertiesUpdatedMessage
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.authentication.CheckIfConnectionIsReadOnly
import com.lasthopesoftware.bluewater.client.connection.url.UrlKeyHolder
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.ManageNowPlayingState
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.NowPlaying
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.viewmodels.NowPlayingFilePropertiesViewModel
import com.lasthopesoftware.bluewater.client.playback.service.ControlPlaybackService
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

private const val libraryId = 697

class WhenItsPropertiesChanges {

	private val playlist = listOf(
		ServiceFile("71"),
		ServiceFile("614"),
		ServiceFile("252"),
		ServiceFile("643"),
		ServiceFile("409"),
		ServiceFile("1000"),
		ServiceFile("188"),
		ServiceFile("118"),
	)

	private val playlistPosition
		get() = 708 % playlist.size

	private val services by lazy {
		val nowPlayingRepository = mockk<ManageNowPlayingState> {
			every { promiseNowPlaying(LibraryId(libraryId)) } returns Promise(
				NowPlaying(
					LibraryId(libraryId),
					playlist,
					playlistPosition,
					439774,
					false
				)
			)
		}

		val filePropertiesProvider = mockk<ProvideFreshLibraryFileProperties> {
			every { promiseFileProperties(LibraryId(libraryId), playlist[playlistPosition]) } returnsMany listOf(
				mapOf(
					Pair(NormalizedFileProperties.Artist, "block"),
					Pair(NormalizedFileProperties.Name, "tongue"),
					Pair(NormalizedFileProperties.Rating, "422"),
				).toPromise(),
				mapOf(
					Pair(NormalizedFileProperties.Artist, "plan"),
					Pair(NormalizedFileProperties.Name, "honor"),
					Pair(NormalizedFileProperties.Rating, "82"),
				).toPromise(),
			)
		}

		val checkAuthentication = mockk<CheckIfConnectionIsReadOnly> {
			every { promiseIsReadOnly(LibraryId(libraryId)) } returns false.toPromise()
		}

		val playbackService = mockk<ControlPlaybackService> {
			every { promiseIsMarkedForPlay(LibraryId(libraryId)) } returns true.toPromise()
		}

		val messageBus = RecordingApplicationMessageBus()

		val nowPlayingViewModel = NowPlayingFilePropertiesViewModel(
            messageBus,
            nowPlayingRepository,
            filePropertiesProvider,
            mockk {
                every { promiseGuaranteedUrlKey(LibraryId(libraryId), playlist[playlistPosition]) } returns Promise(
                    UrlKeyHolder(URL("http://77Q8Tq2h/"), playlist[playlistPosition])
                )
            },
            mockk(),
            checkAuthentication,
            playbackService,
			mockk(),
			mockk(relaxed = true),
			RecordingTypedMessageBus(),
		)

		Pair(messageBus, nowPlayingViewModel)
	}

	private val viewModel
		get() = services.second

	@BeforeAll
	fun act() {
		val (messageBus, viewModel) = services
		viewModel.initializeViewModel(LibraryId(libraryId)).toExpiringFuture().get()
		messageBus.sendMessage(FilePropertiesUpdatedMessage(UrlKeyHolder(URL("http://77Q8Tq2h/"), playlist[playlistPosition])))
	}

	@Test
	fun `then the file position is correct`() {
		assertThat(viewModel.filePosition.value).isEqualTo(439774)
	}

	@Test
	fun `then the artist is correct`() {
		assertThat(viewModel.artist.value).isEqualTo("plan")
	}

	@Test
	fun `then the name is correct`() {
		assertThat(viewModel.title.value).isEqualTo("honor")
	}

	@Test
	fun `then the rating is correct`() {
		assertThat(viewModel.songRating.value).isEqualTo(82f)
	}

	@Test
	fun `then the properties are NOT read only`() {
		assertThat(viewModel.isReadOnly.value).isFalse
	}
}
