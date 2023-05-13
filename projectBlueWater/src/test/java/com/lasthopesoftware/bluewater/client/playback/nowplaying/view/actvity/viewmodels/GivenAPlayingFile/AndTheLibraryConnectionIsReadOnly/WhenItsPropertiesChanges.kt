package com.lasthopesoftware.bluewater.client.playback.nowplaying.view.actvity.viewmodels.GivenAPlayingFile.AndTheLibraryConnectionIsReadOnly

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.properties.KnownFileProperties
import com.lasthopesoftware.bluewater.client.browsing.files.properties.ProvideFreshLibraryFileProperties
import com.lasthopesoftware.bluewater.client.browsing.files.properties.storage.FilePropertiesUpdatedMessage
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.MaintainNowPlayingState
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.NowPlaying
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.viewmodels.NowPlayingFilePropertiesViewModel
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
import java.net.URL

private const val libraryId = 797

class WhenItsPropertiesChanges {

	private val playlist = listOf(
		ServiceFile(746),
		ServiceFile(589),
		ServiceFile(525),
		ServiceFile(340),
		ServiceFile(483),
		ServiceFile(153),
		ServiceFile(358),
		ServiceFile(440),
		ServiceFile(644),
		ServiceFile(113),
	)

	private val playlistPosition
		get() = 354 % playlist.size

	private val services by lazy {
		val nowPlayingRepository = mockk<MaintainNowPlayingState> {
			every { promiseNowPlaying() } returns Promise(
				NowPlaying(
					LibraryId(libraryId),
					playlist,
					playlistPosition,
					816585,
					true
				)
			)
		}

		val filePropertiesProvider = mockk<ProvideFreshLibraryFileProperties> {
			every { promiseFileProperties(LibraryId(libraryId), playlist[playlistPosition]) } returnsMany listOf(
				mapOf(
					Pair(KnownFileProperties.Artist, "english"),
					Pair(KnownFileProperties.Name, "FALSE"),
				).toPromise(),
				mapOf(
					Pair(KnownFileProperties.Artist, "defend"),
					Pair(KnownFileProperties.Name, "sorrow"),
				).toPromise(),
			)
		}

		val messageBus = RecordingApplicationMessageBus()

		val nowPlayingViewModel = NowPlayingFilePropertiesViewModel(
            messageBus,
            nowPlayingRepository,
            filePropertiesProvider,
            mockk {
                every { promiseUrlKey(LibraryId(libraryId), playlist[playlistPosition]) } returns Promise(
                    UrlKeyHolder(URL("http://test"), playlist[playlistPosition])
                )
            },
            mockk(),
            mockk {
                every { promiseIsReadOnly(LibraryId(libraryId)) } returns true.toPromise()
            },
            mockk {
                every { promiseIsMarkedForPlay() } returns false.toPromise()
            },
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
		messageBus.sendMessage(FilePropertiesUpdatedMessage(UrlKeyHolder(URL("http://test"), playlist[playlistPosition])))
	}

	@Test
	fun `then the file position is correct`() {
		assertThat(viewModel.filePosition.value).isEqualTo(816585)
	}

	@Test
	fun `then the artist is correct`() {
		assertThat(viewModel.artist.value).isEqualTo("defend")
	}

	@Test
	fun `then the name is correct`() {
		assertThat(viewModel.title.value).isEqualTo("sorrow")
	}

	@Test
	fun `then the properties are read only`() {
		assertThat(viewModel.isReadOnly.value).isTrue
	}

	@Test
	fun `then playback is marked as repeating`() {
		assertThat(viewModel.isRepeating.value).isTrue
	}
}
