package com.lasthopesoftware.bluewater.client.playback.nowplaying.view.viewmodels.fileproperties.GivenAPlayingFile.AndTheRatingIsChanged

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.properties.MappedFilePropertiesLookup
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
import com.lasthopesoftware.bluewater.shared.promises.extensions.DeferredPromise
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

private const val libraryId = 798

class WhenTheRatingPropertyChangesConcurrently {

	private val playlist = listOf(
		ServiceFile("408"),
		ServiceFile("480"),
		ServiceFile("124"),
		ServiceFile("224"),
		ServiceFile("567"),
		ServiceFile("235"),
		ServiceFile("656"),
		ServiceFile("882"),
		ServiceFile("848"),
	)

	private val playlistPosition by lazy { 831 % playlist.size }

	private val serviceFile by lazy { playlist[playlistPosition] }

	private val deferredFilePropertiesPromise = DeferredPromise(Unit)

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
			every { promiseFileProperties(LibraryId(libraryId), serviceFile) } returnsMany listOf(
				MappedFilePropertiesLookup(mapOf(
					Pair(NormalizedFileProperties.Artist, "within"),
					Pair(NormalizedFileProperties.Name, "descent"),
					Pair(NormalizedFileProperties.Rating, "633"),
				)).toPromise(),
				MappedFilePropertiesLookup(mapOf(
					Pair(NormalizedFileProperties.Artist, "want"),
					Pair(NormalizedFileProperties.Name, "toward"),
					Pair(NormalizedFileProperties.Rating, "899"),
				)).toPromise(),
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
                every { promiseGuaranteedUrlKey(LibraryId(libraryId), serviceFile) } returns Promise(
                    UrlKeyHolder(URL("http://77Q8Tq2h/"), serviceFile)
                )
            },
            mockk {
				every { promiseFileUpdate(LibraryId(libraryId), serviceFile, NormalizedFileProperties.Rating, any(), any()) } returns deferredFilePropertiesPromise
			},
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
		viewModel.updateRating(201.64f)
		viewModel.updateRating(694.34f)
		messageBus.sendMessage(FilePropertiesUpdatedMessage(UrlKeyHolder(URL("http://77Q8Tq2h/"), serviceFile)))
		deferredFilePropertiesPromise.resolve()
	}

	@Test
	fun `then the file position is correct`() {
		assertThat(viewModel.filePosition.value).isEqualTo(439774)
	}

	@Test
	fun `then the artist is correct`() {
		assertThat(viewModel.artist.value).isEqualTo("want")
	}

	@Test
	fun `then the name is correct`() {
		assertThat(viewModel.title.value).isEqualTo("toward")
	}

	@Test
	fun `then the properties are NOT read only`() {
		assertThat(viewModel.isReadOnly.value).isFalse
	}

	@Test
	fun `then the rating is correct`() {
		assertThat(viewModel.songRating.value).isEqualTo(694.34f)
	}
}
