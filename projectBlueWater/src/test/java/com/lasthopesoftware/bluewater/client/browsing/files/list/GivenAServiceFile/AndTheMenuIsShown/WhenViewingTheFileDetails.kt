package com.lasthopesoftware.bluewater.client.browsing.files.list.GivenAServiceFile.AndTheMenuIsShown

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.details.LaunchFileDetails
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

class WhenViewingTheFileDetails {

	private var launchedFile: ServiceFile? = null

	private val viewModel by lazy {
		val filePropertiesProvider = mockk<ProvideScopedFileProperties>().apply {
			every { promiseFileProperties(ServiceFile(34)) } returns mapOf(
				Pair("Artist", "adopt"),
				Pair("Name", "lovely"),
			).toPromise()
		}

		val stringResource = mockk<GetStringResources>().apply {
			every { loading } returns "native"
			every { unknownArtist } returns "receive"
			every { unknownTrack } returns "pack"
		}

		val launchFileDetails = mockk<LaunchFileDetails>().apply {
			every { launchFileDetails(any()) } answers {
				launchedFile = firstArg()
			}
		}

		ReusableTrackHeadlineViewModel(
			filePropertiesProvider,
			stringResource,
			mockk(),
			launchFileDetails,
			RecordingTypedMessageBus(),
		)
	}

	@BeforeAll
	fun act() {
		viewModel.promiseUpdate(ServiceFile(34)).toExpiringFuture().get()
		viewModel.showMenu()
		viewModel.viewFileDetails()
	}

	@Test
	fun thenTheArtistIsCorrect() {
		assertThat(viewModel.artist.value).isEqualTo("adopt")
	}

	@Test
	fun thenTheTrackNameIsCorrect() {
		assertThat(viewModel.title.value).isEqualTo("lovely")
	}

	@Test
	fun thenTheLaunchedFileIsCorrect() {
		assertThat(launchedFile).isEqualTo(ServiceFile(34))
	}

	@Test
	fun `then the menu is hidden`() {
		assertThat(viewModel.isMenuShown.value).isFalse
	}
}
