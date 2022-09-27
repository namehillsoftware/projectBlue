package com.lasthopesoftware.bluewater.client.playback.view.nowplaying.activity.viewmodels.GivenAPlayingFile.AndTheRatingIsChanged

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.properties.KnownFileProperties
import com.lasthopesoftware.bluewater.client.browsing.files.properties.ProvideLibraryFileProperties
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.authentication.CheckIfConnectionIsReadOnly
import com.lasthopesoftware.bluewater.client.playback.file.PositionedFile
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.MaintainNowPlayingState
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.NowPlaying
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.activity.viewmodels.NowPlayingFilePropertiesViewModel
import com.lasthopesoftware.bluewater.client.playback.service.ControlPlaybackService
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.messages.PlaybackMessage
import com.lasthopesoftware.bluewater.shared.UrlKeyHolder
import com.lasthopesoftware.bluewater.shared.promises.extensions.DeferredPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
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
		val nowPlayingRepository = mockk<MaintainNowPlayingState> {
			every { promiseNowPlaying() } returns Promise(
				NowPlaying(
					LibraryId(libraryId),
					playlist,
					firstPlaylistPosition,
					439774,
					false
				)
			) andThen Promise(
				NowPlaying(
					LibraryId(libraryId),
					playlist,
					firstPlaylistPosition,
					439774,
					false
				)
			) andThen Promise(
				NowPlaying(
					LibraryId(libraryId),
					playlist,
					secondPlaylistPosition,
					558,
					false
				)
			)
		}

		val filePropertiesProvider = mockk<ProvideLibraryFileProperties> {
			every { promiseFileProperties(LibraryId(libraryId), firstServiceFile) } returns
				mapOf(
					Pair(KnownFileProperties.ARTIST, "accident"),
					Pair(KnownFileProperties.NAME, "proposal"),
					Pair(KnownFileProperties.RATING, "107"),
				).toPromise()

			every { promiseFileProperties(LibraryId(libraryId), secondServiceFile) } returns
				mapOf(
					Pair(KnownFileProperties.ARTIST, "offer"),
					Pair(KnownFileProperties.NAME, "film"),
					Pair(KnownFileProperties.RATING, "476"),
				).toPromise()
		}

		val checkAuthentication = mockk<CheckIfConnectionIsReadOnly> {
			every { promiseIsReadOnly(LibraryId(libraryId)) } returns false.toPromise()
		}

		val playbackService = mockk<ControlPlaybackService> {
			every { promiseIsMarkedForPlay() } returns true.toPromise()
		}

		val messageBus = RecordingApplicationMessageBus()

		val nowPlayingViewModel = NowPlayingFilePropertiesViewModel(
            messageBus,
            nowPlayingRepository,
            filePropertiesProvider,
            mockk {
                every { promiseUrlKey(LibraryId(libraryId), firstServiceFile) } returns Promise(
                    UrlKeyHolder(URL("http://77Q8Tq2h/"), firstServiceFile)
                )

				every { promiseUrlKey(LibraryId(libraryId), secondServiceFile) } returns Promise(
					UrlKeyHolder(URL("http://77Q8Tq2h/"), secondServiceFile)
				)
            },
            mockk {
				every { promiseFileUpdate(LibraryId(libraryId), firstServiceFile, KnownFileProperties.RATING, any(), any()) } returns deferredFilePropertiesPromise
			},
            checkAuthentication,
            playbackService,
            mockk(),
            mockk(relaxed = true),
		)

		Pair(messageBus, nowPlayingViewModel)
	}

	private val viewModel
		get() = services.second

	@BeforeAll
	fun act() {
		val (messageBus, viewModel) = services
		viewModel.initializeViewModel().toExpiringFuture().get()
		viewModel.updateRating(201.64f)
		messageBus.sendMessage(PlaybackMessage.TrackChanged(LibraryId(libraryId), PositionedFile(secondPlaylistPosition, secondServiceFile)))
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
	fun `then playback is NOT marked as repeating`() {
		assertThat(viewModel.isRepeating.value).isFalse
	}

	@Test
	fun `then the rating is correct`() {
		assertThat(viewModel.songRating.value).isEqualTo(476f)
	}
}
