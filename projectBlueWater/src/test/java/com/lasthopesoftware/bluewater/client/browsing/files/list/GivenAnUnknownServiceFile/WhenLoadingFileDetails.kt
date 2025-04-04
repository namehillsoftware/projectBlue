package com.lasthopesoftware.bluewater.client.browsing.files.list.GivenAnUnknownServiceFile

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.list.ReusableFileViewModel
import com.lasthopesoftware.bluewater.client.browsing.files.list.ReusablePlaylistFileViewModel
import com.lasthopesoftware.bluewater.client.browsing.files.properties.ProvideLibraryFileProperties
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.url.UrlKeyHolder
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.promises.extensions.toPromise
import com.lasthopesoftware.resources.RecordingApplicationMessageBus
import com.lasthopesoftware.resources.RecordingTypedMessageBus
import com.lasthopesoftware.resources.strings.GetStringResources
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.net.URL

private const val libraryId = 487

class WhenLoadingFileDetails {

	private val viewModel by lazy {
		val filePropertiesProvider = mockk<ProvideLibraryFileProperties>().apply {
			every { promiseFileProperties(LibraryId(libraryId), any()) } returns emptyMap<String, String>().toPromise()
		}

		val stringResource = mockk<GetStringResources>().apply {
			every { loading } returns "waiter"
			every { unknownArtist } returns "bunch"
			every { unknownTrack } returns "bold"
		}

		ReusablePlaylistFileViewModel(
			RecordingTypedMessageBus(),
			ReusableFileViewModel(
				filePropertiesProvider,
				stringResource,
				mockk {
					every { promiseUrlKey(LibraryId(libraryId), any<ServiceFile>()) } answers {
						Promise(
							UrlKeyHolder(URL("http://test"), arg(1))
						)
					}
				},
				RecordingApplicationMessageBus(),
			),
		)
	}

	@BeforeAll
	fun act() {
		viewModel.promiseUpdate(LibraryId(libraryId), ServiceFile("943")).toExpiringFuture().get()
	}

	@Test
	fun thenTheArtistIsCorrect() {
		assertThat(viewModel.artist.value).isEqualTo("bunch")
	}

	@Test
	fun thenTheTrackNameIsCorrect() {
		assertThat(viewModel.title.value).isEqualTo("bold")
	}
}
