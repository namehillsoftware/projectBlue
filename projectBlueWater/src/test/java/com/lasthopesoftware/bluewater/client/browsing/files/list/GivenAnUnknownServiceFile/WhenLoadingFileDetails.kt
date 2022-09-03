package com.lasthopesoftware.bluewater.client.browsing.files.list.GivenAnUnknownServiceFile

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.list.ReusableTrackHeadlineViewModel
import com.lasthopesoftware.bluewater.client.browsing.files.properties.ProvideScopedFileProperties
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.lasthopesoftware.resources.RecordingTypedMessageBus
import com.lasthopesoftware.resources.strings.GetStringResources
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class WhenLoadingFileDetails {

	private val viewModel by lazy {
		val filePropertiesProvider = mockk<ProvideScopedFileProperties>().apply {
			every { promiseFileProperties(any()) } returns emptyMap<String, String>().toPromise()
		}

		val stringResource = mockk<GetStringResources>().apply {
			every { loading } returns "waiter"
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

	@BeforeAll
	fun act() {
		viewModel.promiseUpdate(
			listOf(
				ServiceFile(943)
			),
			0
		).toExpiringFuture().get()
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
