package com.lasthopesoftware.bluewater.client.playback.view.nowplaying.activity.viewmodels.GivenAPlayingFile.AndTheRatingIsChanged

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.properties.KnownFileProperties
import com.lasthopesoftware.bluewater.client.browsing.files.properties.ProvideLibraryFileProperties
import com.lasthopesoftware.bluewater.client.browsing.files.properties.storage.FilePropertiesUpdatedMessage
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.authentication.CheckIfConnectionIsReadOnly
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.MaintainNowPlayingState
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.NowPlaying
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.activity.viewmodels.NowPlayingFilePropertiesViewModel
import com.lasthopesoftware.bluewater.client.playback.service.ControlPlaybackService
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

private const val libraryId = 798

class WhenTheRatingPropertyChangesConcurrently {

	private val playlist = listOf(
		ServiceFile(408),
		ServiceFile(480),
		ServiceFile(124),
		ServiceFile(224),
		ServiceFile(567),
		ServiceFile(235),
		ServiceFile(656),
		ServiceFile(882),
		ServiceFile(848),
	)

	private val playlistPosition by lazy { 831 % playlist.size }

	private val serviceFile by lazy { playlist[playlistPosition] }

	private val deferredFilePropertiesPromise = DeferredPromise(Unit)

	private val services by lazy {
		val nowPlayingRepository = mockk<MaintainNowPlayingState> {
			every { promiseNowPlaying() } returns Promise(
				NowPlaying(
					LibraryId(libraryId),
					playlist,
					playlistPosition,
					439774,
					false
				)
			)
		}

		val filePropertiesProvider = mockk<ProvideLibraryFileProperties> {
			every { promiseFileProperties(LibraryId(libraryId), serviceFile) } returnsMany listOf(
				mapOf(
					Pair(KnownFileProperties.Artist, "within"),
					Pair(KnownFileProperties.Name, "descent"),
					Pair(KnownFileProperties.Rating, "633"),
				).toPromise(),
				mapOf(
					Pair(KnownFileProperties.Artist, "want"),
					Pair(KnownFileProperties.Name, "toward"),
					Pair(KnownFileProperties.Rating, "899"),
				).toPromise(),
			)
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
                every { promiseUrlKey(LibraryId(libraryId), serviceFile) } returns Promise(
                    UrlKeyHolder(URL("http://77Q8Tq2h/"), serviceFile)
                )
            },
            mockk {
				every { promiseFileUpdate(LibraryId(libraryId), serviceFile, KnownFileProperties.Rating, any(), any()) } returns deferredFilePropertiesPromise
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
	fun `then playback is NOT marked as repeating`() {
		assertThat(viewModel.isRepeating.value).isFalse
	}

	@Test
	fun `then the rating is correct`() {
		assertThat(viewModel.songRating.value).isEqualTo(694.34f)
	}
}
