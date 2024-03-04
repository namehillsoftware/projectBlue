package com.lasthopesoftware.bluewater.client.browsing.files.list.GivenNoServiceFile

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.list.ReusableFileViewModel
import com.lasthopesoftware.bluewater.client.browsing.files.list.ReusablePlaylistFileViewModel
import com.lasthopesoftware.bluewater.client.browsing.files.properties.ProvideLibraryFileProperties
import com.lasthopesoftware.bluewater.shared.UrlKeyHolder
import com.lasthopesoftware.promises.extensions.toPromise
import com.lasthopesoftware.resources.RecordingApplicationMessageBus
import com.lasthopesoftware.resources.RecordingTypedMessageBus
import com.lasthopesoftware.resources.strings.GetStringResources
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.net.URL

class WhenViewingTrackHeadline {

	private val viewModel by lazy {
		val filePropertiesProvider = mockk<ProvideLibraryFileProperties>().apply {
			every { promiseFileProperties(any(), any()) } returns emptyMap<String, String>().toPromise()
		}

		val stringResource = mockk<GetStringResources>().apply {
			every { loading } returns "animal"
			every { unknownArtist } returns "bunch"
			every { unknownTrack } returns "bold"
		}

		ReusablePlaylistFileViewModel(
			RecordingTypedMessageBus(),
			ReusableFileViewModel(
				filePropertiesProvider,
				stringResource,
				mockk {
					every { promiseUrlKey(any(), any<ServiceFile>()) } answers {
						Promise(
							UrlKeyHolder(URL("http://test"), firstArg())
						)
					}
				},
				RecordingApplicationMessageBus(),
			),
		)
	}

	@Test
	fun thenTheArtistIsCorrect() {
		assertThat(viewModel.artist.value).isEqualTo("")
	}

	@Test
	fun thenTheTrackNameIsCorrect() {
		assertThat(viewModel.title.value).isEqualTo("animal")
	}
}
