package com.lasthopesoftware.bluewater.client.browsing.files.list.GivenAQuery

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.list.SearchFilesViewModel
import com.lasthopesoftware.bluewater.client.browsing.files.properties.ReadOnlyFileProperty
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

private const val libraryId = 421

class `When Prepending A Filter` {
	private val viewModel by lazy {
		val vm = SearchFilesViewModel(
			mockk {
				every { promiseAudioFiles(LibraryId(libraryId), "[FhFuGYramwv]=YVUf8Q83 T8Dtib8Rh") } returns
					Promise(
						listOf(
							ServiceFile("438"),
							ServiceFile("340"),
							ServiceFile("249"),
						)
					)
			},
		)

		vm
	}

	@BeforeAll
	fun act() {
		viewModel.setActiveLibraryId(LibraryId(libraryId))
		viewModel.query.value = "T8Dtib8Rh"
		viewModel.prependFilter(ReadOnlyFileProperty("FhFuGYramwv", "YVUf8Q83"))
		viewModel.findFiles().toExpiringFuture().get()
	}

	@Test
	fun `then the query is correct`() {
		assertThat(viewModel.query.value).isEqualTo("[FhFuGYramwv]=YVUf8Q83 T8Dtib8Rh")
	}

	@Test fun `then the results are correct`() {
		assertThat(viewModel.files.value).hasSameElementsAs(
			listOf(
				ServiceFile("438"),
				ServiceFile("340"),
				ServiceFile("249"),
			)
		)
	}
}
