package com.lasthopesoftware.bluewater.client.browsing.files.list.GivenNoServiceFile

import com.lasthopesoftware.bluewater.client.browsing.files.list.ReusableTrackHeadlineViewModel
import com.lasthopesoftware.bluewater.client.browsing.files.properties.ProvideScopedFileProperties
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.lasthopesoftware.resources.RecordingTypedMessageBus
import com.lasthopesoftware.resources.strings.GetStringResources
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class WhenViewingTrackHeadline {

	private val viewModel by lazy {
		val filePropertiesProvider = mockk<ProvideScopedFileProperties>().apply {
			every { promiseFileProperties(any()) } returns emptyMap<String, String>().toPromise()
		}

		val stringResource = mockk<GetStringResources>().apply {
			every { loading } returns "animal"
			every { unknownArtist } returns "bunch"
			every { unknownTrack } returns "bold"
		}

		ReusableTrackHeadlineViewModel(
			filePropertiesProvider,
			stringResource,
			mockk(),
			mockk(),
			RecordingTypedMessageBus(),
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