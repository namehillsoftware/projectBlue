package com.lasthopesoftware.bluewater.client.playback.nowplaying.view.viewmodels.fileproperties.GivenAPlayingFile.AndAnInitializedViewModel.AndTheLibraryIsChangedToOneWithNothingPlaying

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.properties.NormalizedFileProperties
import com.lasthopesoftware.bluewater.client.browsing.files.properties.ProvideFreshLibraryFileProperties
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.authentication.CheckIfConnectionIsReadOnly
import com.lasthopesoftware.bluewater.client.connection.url.UrlKeyHolder
import com.lasthopesoftware.bluewater.client.playback.file.PositionedFile
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.ManageNowPlayingState
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.NowPlaying
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.NowPlayingMessage
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.viewmodels.NowPlayingFilePropertiesViewModel
import com.lasthopesoftware.bluewater.client.playback.service.ControlPlaybackService
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.promises.extensions.toPromise
import com.lasthopesoftware.resources.RecordingTypedMessageBus
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.net.URL

class `When changing the library back` {

	companion object {
		private const val libraryId = 30
		private const val newLibraryId = 127
		private const val serviceFileId = "873"
	}

	private val mut by lazy {
		val nowPlayingRepository = mockk<ManageNowPlayingState> {
			every { promiseNowPlaying(LibraryId(libraryId)) } returns Promise(
				NowPlaying(
					LibraryId(libraryId),
					listOf(
						ServiceFile("815"),
						ServiceFile("449"),
						ServiceFile(serviceFileId),
						ServiceFile("592"),
						ServiceFile("390"),
					),
					2,
					607206440,
					false
				)
			)

			every { promiseNowPlaying(LibraryId(newLibraryId)) } returns Promise(
				NowPlaying(
					LibraryId(newLibraryId),
					emptyList(),
					-1,
					0,
					false
				)
			)
		}

		val filePropertiesProvider = mockk<ProvideFreshLibraryFileProperties> {
			every { promiseFileProperties(LibraryId(libraryId), ServiceFile(serviceFileId)) } answers {
				mapOf(
					Pair(NormalizedFileProperties.Artist, "M4Znyape"),
					Pair(NormalizedFileProperties.Name, "6Treuhhy"),
					Pair(NormalizedFileProperties.Duration, "60"),
					Pair(NormalizedFileProperties.Rating, "964.27"),
				).toPromise()
			}
		}

		val checkAuthentication = mockk<CheckIfConnectionIsReadOnly> {
			every { promiseIsReadOnly(LibraryId(libraryId)) } returns true.toPromise()
		}

		val playbackService = mockk<ControlPlaybackService> {
			every { promiseIsMarkedForPlay(LibraryId(libraryId)) } returns true.toPromise()
			every { promiseIsMarkedForPlay(LibraryId(newLibraryId)) } returns true.toPromise()
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

		nowPlayingViewModel
	}

	@BeforeAll
	fun act() {
		mut.initializeViewModel(LibraryId(libraryId)).toExpiringFuture().get()
		mut.initializeViewModel(LibraryId(newLibraryId)).toExpiringFuture().get()
		mut.initializeViewModel(LibraryId(libraryId)).toExpiringFuture().get()
	}

	@Test
	fun `then the artist is correct`() {
		assertThat(mut.artist.value).isEqualTo("M4Znyape")
	}

	@Test
	fun `then the title is correct`() {
		assertThat(mut.title.value).isEqualTo("6Treuhhy")
	}

	@Test
	fun `then the rating is correct`() {
		assertThat(mut.songRating.value).isEqualTo(964.27f)
	}

	@Test
	fun `then the duration is correct`() {
		assertThat(mut.fileDuration.value).isEqualTo(60000)
	}

	@Test
	fun `then the playing file is correct`() {
		assertThat(mut.nowPlayingFile.value).isEqualTo(PositionedFile(2, ServiceFile(serviceFileId)))
	}

	@Test
	fun `then the file position is correct`() {
		assertThat(mut.filePosition.value).isEqualTo(607206440)
	}
}
