package com.lasthopesoftware.bluewater.client.browsing.files.list.GivenAServiceFile.AndTheMenuIsShown

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.list.ReusableTrackHeadlineViewModel
import com.lasthopesoftware.bluewater.client.browsing.files.properties.ProvideScopedFileProperties
import com.lasthopesoftware.bluewater.client.playback.service.ControlPlaybackService
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

class WhenAddingTheFileToNowPlaying {

	private var addedFile: ServiceFile? = null

	private val viewModel by lazy {
		val filePropertiesProvider = mockk<ProvideScopedFileProperties> {
			every { promiseFileProperties(ServiceFile(483)) } returns mapOf(
				Pair("Artist", "beg"),
				Pair("Name", "prize"),
			).toPromise()
		}

		val stringResource = mockk<GetStringResources> {
			every { loading } returns "spirit"
			every { unknownArtist } returns "mean"
			every { unknownTrack } returns "business"
		}

		val controlNowPlaying = mockk<ControlPlaybackService> {
			every { addToPlaylist(ServiceFile(483)) } answers {
				addedFile = firstArg()
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
			controlNowPlaying,
			mockk(),
			RecordingTypedMessageBus(),
			RecordingApplicationMessageBus(),
		)
	}

	@BeforeAll
	fun act() {
		viewModel.promiseUpdate(
			listOf(
				ServiceFile(164),
				ServiceFile(622),
				ServiceFile(14),
				ServiceFile(483),
				ServiceFile(486),
			),
			3
		).toExpiringFuture().get()
		viewModel.showMenu()
		viewModel.addToNowPlaying()
	}

	@Test
	fun thenTheArtistIsCorrect() {
		assertThat(viewModel.artist.value).isEqualTo("beg")
	}

	@Test
	fun thenTheTrackNameIsCorrect() {
		assertThat(viewModel.title.value).isEqualTo("prize")
	}

	@Test
	fun thenTheFileIsAddedToNowPlaying() {
		assertThat(addedFile).isEqualTo(ServiceFile(483))
	}

	@Test
	fun `then the menu is hidden`() {
		assertThat(viewModel.isMenuShown.value).isFalse
	}
}
