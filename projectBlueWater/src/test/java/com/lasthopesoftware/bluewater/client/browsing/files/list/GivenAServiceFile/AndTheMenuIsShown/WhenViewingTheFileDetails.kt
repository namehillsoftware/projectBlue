package com.lasthopesoftware.bluewater.client.browsing.files.list.GivenAServiceFile.AndTheMenuIsShown

import com.lasthopesoftware.bluewater.NavigateApplication
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.list.ReusableTrackHeadlineViewModel
import com.lasthopesoftware.bluewater.client.browsing.files.properties.ProvideScopedFileProperties
import com.lasthopesoftware.bluewater.shared.UrlKeyHolder
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
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

class WhenViewingTheFileDetails {

	private var launchedFile: ServiceFile? = null

	private val viewModel by lazy {
		val filePropertiesProvider = mockk<ProvideScopedFileProperties> {
			every { promiseFileProperties(ServiceFile(34)) } returns mapOf(
				Pair("Artist", "adopt"),
				Pair("Name", "lovely"),
			).toPromise()
		}

		val stringResource = mockk<GetStringResources> {
			every { loading } returns "native"
			every { unknownArtist } returns "receive"
			every { unknownTrack } returns "pack"
		}

		val launchFileDetails = mockk<NavigateApplication> {
			every { viewFileDetails(any(), any()) } answers {
				val files = firstArg<List<ServiceFile>>()
				launchedFile = files[lastArg()]
			}
		}

		ReusableTrackHeadlineViewModel(
			filePropertiesProvider,
			mockk {
				every { promiseUrlKey(any<ServiceFile>()) } answers {
					Promise(
						UrlKeyHolder(URL("http://test"), firstArg())
					)
				}
			},
			stringResource,
			mockk(),
			launchFileDetails,
			RecordingTypedMessageBus(),
			RecordingApplicationMessageBus(),
		)
	}

	@BeforeAll
	fun act() {
		viewModel.promiseUpdate(
			listOf(
				ServiceFile(401),
				ServiceFile(223),
				ServiceFile(913),
				ServiceFile(464),
				ServiceFile(734),
				ServiceFile(761),
				ServiceFile(34),
				ServiceFile(872),
				ServiceFile(350),
				ServiceFile(786),
				ServiceFile(368),
			),
			6
		).toExpiringFuture().get()
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
