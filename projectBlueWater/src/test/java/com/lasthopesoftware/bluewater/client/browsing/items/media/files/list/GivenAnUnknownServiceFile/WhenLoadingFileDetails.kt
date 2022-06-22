package com.lasthopesoftware.bluewater.client.browsing.items.media.files.list.GivenAnUnknownServiceFile

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.list.TrackHeadlineViewModel
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.ProvideScopedFileProperties
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.lasthopesoftware.resources.strings.GetStringResources
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.BeforeClass
import org.junit.Test

private val viewModel by lazy {
	val filePropertiesProvider = mockk<ProvideScopedFileProperties>().apply {
		every { promiseFileProperties(any()) } returns emptyMap<String, String>().toPromise()
	}

	val stringResource = mockk<GetStringResources>().apply {
		every { loading } returns "waiter"
		every { unknownArtist } returns "bunch"
		every { unknownTrack } returns "bold"
	}

	TrackHeadlineViewModel(
		filePropertiesProvider,
		stringResource,
		mockk(),
		mockk()
	)
}

class WhenLoadingFileDetails {

	companion object {
		@JvmStatic
		@BeforeClass
		fun act() {
			viewModel.promiseUpdate(ServiceFile(943)).toExpiringFuture().get()
		}
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
