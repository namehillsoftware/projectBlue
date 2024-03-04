package com.lasthopesoftware.bluewater.client.playback.nowplaying.view.viewmodels.fileproperties.GivenAPlayingFile.AndTheRatingIsChanged

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.properties.KnownFileProperties
import com.lasthopesoftware.bluewater.client.browsing.files.properties.ProvideFreshLibraryFileProperties
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.authentication.CheckIfConnectionIsReadOnly
import com.lasthopesoftware.bluewater.client.playback.file.PositionedFile
import com.lasthopesoftware.bluewater.client.playback.nowplaying.FakeNowPlayingRepository
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.NowPlaying
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.viewmodels.NowPlayingFilePropertiesViewModel
import com.lasthopesoftware.bluewater.client.playback.service.ControlPlaybackService
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.messages.LibraryPlaybackMessage
import com.lasthopesoftware.bluewater.shared.UrlKeyHolder
import com.lasthopesoftware.bluewater.shared.promises.extensions.DeferredPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.promises.extensions.toPromise
import com.lasthopesoftware.resources.RecordingApplicationMessageBus
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.net.URL

private const val libraryId = 681

class WhenTheServiceFileChangesConcurrently {

	private val playlist = listOf(
		ServiceFile(738),
		ServiceFile(441),
		ServiceFile(595),
		ServiceFile(285),
		ServiceFile(833),
		ServiceFile(78),
		ServiceFile(819),
		ServiceFile(386),
		ServiceFile(989),
	)

	private val firstPlaylistPosition by lazy { 945 % playlist.size }
	private val firstServiceFile by lazy { playlist[firstPlaylistPosition] }

	private val secondPlaylistPosition by lazy { 116 % playlist.size }
	private val secondServiceFile by lazy { playlist[secondPlaylistPosition] }

	private val deferredFilePropertiesPromise = DeferredPromise(Unit)

	private val services by lazy {
		val nowPlaying = NowPlaying(
			LibraryId(libraryId),
			playlist,
			firstPlaylistPosition,
			439774,
			false
		)

		val nowPlayingRepository = FakeNowPlayingRepository(nowPlaying)

		val filePropertiesProvider = mockk<ProvideFreshLibraryFileProperties> {
			every { promiseFileProperties(LibraryId(libraryId), firstServiceFile) } returns
				mapOf(
					Pair(KnownFileProperties.Artist, "accident"),
					Pair(KnownFileProperties.Name, "proposal"),
					Pair(KnownFileProperties.Rating, "107"),
				).toPromise()

			every { promiseFileProperties(LibraryId(libraryId), secondServiceFile) } returns
				mapOf(
					Pair(KnownFileProperties.Artist, "offer"),
					Pair(KnownFileProperties.Name, "film"),
					Pair(KnownFileProperties.Rating, "476"),
				).toPromise()
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
                every { promiseGuaranteedUrlKey(LibraryId(libraryId), any<ServiceFile>()) } answers {
					Promise(UrlKeyHolder(URL("http://77Q8Tq2h/"), lastArg()))
				}
            },
            mockk {
				every { promiseFileUpdate(LibraryId(libraryId), firstServiceFile, KnownFileProperties.Rating, any(), any()) } returns deferredFilePropertiesPromise
			},
            checkAuthentication,
            playbackService,
			mockk(),
			mockk(relaxed = true),
		)

		Triple(messageBus, nowPlayingRepository, nowPlayingViewModel)
	}

	private val viewModel
		get() = services.third

	@BeforeAll
	fun act() {
		val (messageBus, nowPlayingRepository, viewModel) = services
		viewModel.initializeViewModel(LibraryId(libraryId)).toExpiringFuture().get()
		viewModel.updateRating(201.64f)
		nowPlayingRepository.updateNowPlaying(nowPlayingRepository.promiseNowPlaying(LibraryId(libraryId)).toExpiringFuture().get()!!.copy(playlistPosition = secondPlaylistPosition, filePosition = 558))
		messageBus.sendMessage(LibraryPlaybackMessage.TrackChanged(LibraryId(libraryId), PositionedFile(secondPlaylistPosition, secondServiceFile)))
		deferredFilePropertiesPromise.resolve()
	}

	@Test
	fun `then the file position is correct`() {
		assertThat(viewModel.filePosition.value).isEqualTo(558)
	}

	@Test
	fun `then the artist is correct`() {
		assertThat(viewModel.artist.value).isEqualTo("offer")
	}

	@Test
	fun `then the name is correct`() {
		assertThat(viewModel.title.value).isEqualTo("film")
	}

	@Test
	fun `then the properties are NOT read only`() {
		assertThat(viewModel.isReadOnly.value).isFalse
	}

	@Test
	fun `then the rating is correct`() {
		assertThat(viewModel.songRating.value).isEqualTo(476f)
	}
}
